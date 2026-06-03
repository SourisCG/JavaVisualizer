import * as vscode from 'vscode';
import { createWebviewPanel } from '../webview/WebviewProvider';
import { ProcessManager } from '../process/ProcessManager';
import { ProjectDetector } from '../process/ProjectDetector';
import { WebSocketBridge } from '../websocket/WebSocketBridge';
import { getSettings } from '../config/settings';

let processManager: ProcessManager | null = null;
let wsBridge: WebSocketBridge | null = null;
let fileWatcher: vscode.FileSystemWatcher | null = null;

export async function openPreview(context: vscode.ExtensionContext) {
    const panel = createWebviewPanel(context);

    if (!processManager) {
        processManager = new ProcessManager(context);
    }

    if (!processManager.isJdkAvailable()) {
        panel.webview.postMessage({
            type: 'showError',
            title: 'JDK Not Found',
            details: 'Could not find Java Development Kit.\n\nPlease install JDK 17+ and set JAVA_HOME, or configure javavisualizer.javaHome in settings.'
        });
        return;
    }

    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        panel.webview.postMessage({
            type: 'showError',
            title: 'No Workspace',
            details: 'Please open a folder containing a Java project.'
        });
        return;
    }

    panel.webview.postMessage({ type: 'setStatus', status: 'compiling', text: 'Compiling...' });

    if (!wsBridge) {
        wsBridge = new WebSocketBridge();
        const port = await wsBridge.start();

        wsBridge.onJavaConnect = () => {
            panel.webview.postMessage({ type: 'setStatus', status: 'connected', text: 'Connected' });
            panel.webview.postMessage({ type: 'hideError' });
        };

        wsBridge.onJavaDisconnect = () => {
            panel.webview.postMessage({ type: 'setStatus', status: '', text: 'Disconnected' });
        };

        panel.webview.postMessage({ type: 'connectWebSocket', port });
    }

    const project = ProjectDetector.detect(workspaceFolder);
    const result = await processManager.compileAndRun(project, true);

    if (!result.success) {
        panel.webview.postMessage({
            type: 'showError',
            title: 'Compilation Error',
            details: result.errors.join('\n')
        });
        return;
    }

    panel.webview.postMessage({ type: 'hideError' });

    setupFileWatcher(context);

    panel.webview.onDidReceiveMessage(async (message) => {
        switch (message.type) {
            case 'refresh':
                await refreshPreviewInternal();
                break;
            case 'runDesktop':
                await runDesktopInternal();
                break;
            case 'autoReload':
                const config = vscode.workspace.getConfiguration('javavisualizer');
                await config.update('autoReload', message.value, vscode.ConfigurationTarget.Workspace);
                break;
        }
    });

    const settings = getSettings();
    panel.webview.postMessage({ type: 'setAutoReload', value: settings.autoReload });
}

function setupFileWatcher(context: vscode.ExtensionContext) {
    if (fileWatcher) {
        fileWatcher.dispose();
    }

    fileWatcher = vscode.workspace.createFileSystemWatcher('**/*.java');

    fileWatcher.onDidChange(async () => {
        const settings = getSettings();
        if (settings.autoReload) {
            await refreshPreviewInternal();
        }
    });

    fileWatcher.onDidCreate(async () => {
        const settings = getSettings();
        if (settings.autoReload) {
            await refreshPreviewInternal();
        }
    });

    fileWatcher.onDidDelete(async () => {
        const settings = getSettings();
        if (settings.autoReload) {
            await refreshPreviewInternal();
        }
    });

    context.subscriptions.push(fileWatcher);
}

async function refreshPreviewInternal() {
    if (!processManager || !wsBridge) {
        return;
    }

    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        return;
    }

    const panel = (await import('../webview/WebviewProvider')).getWebviewPanel();
    if (!panel) {
        return;
    }

    panel.webview.postMessage({ type: 'setStatus', status: 'compiling', text: 'Compiling...' });

    const project = ProjectDetector.detect(workspaceFolder);
    const result = await processManager.compileAndRun(project, true);

    if (!result.success) {
        panel.webview.postMessage({
            type: 'showError',
            title: 'Compilation Error',
            details: result.errors.join('\n')
        });
        return;
    }

    panel.webview.postMessage({ type: 'hideError' });
    panel.webview.postMessage({ type: 'connectWebSocket', port: wsBridge.getPort() });
}

async function runDesktopInternal() {
    if (!processManager) {
        return;
    }

    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        vscode.window.showErrorMessage('No workspace folder open.');
        return;
    }

    const project = ProjectDetector.detect(workspaceFolder);

    const compileResult = await processManager.compileAndRun(project, false);
    if (!compileResult.success) {
        vscode.window.showErrorMessage(`Failed to run: ${compileResult.errors.join('\n')}`);
    }
}
