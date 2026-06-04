import * as vscode from 'vscode';
import { createWebviewPanel, getWebviewPanel, setOnDispose } from '../webview/WebviewProvider';
import { ProcessManager } from '../process/ProcessManager';
import { ProjectDetector } from '../process/ProjectDetector';
import { WebSocketBridge } from '../websocket/WebSocketBridge';
import { getSettings } from '../config/settings';
import { logger } from '../utils/logger';

let processManager: ProcessManager | null = null;
let wsBridge: WebSocketBridge | null = null;
let fileWatcher: vscode.FileSystemWatcher | null = null;
let isCompiling = false;
let debounceTimer: NodeJS.Timeout | null = null;
let messageHandlerRegistered = false;

export function getProcessManager(): ProcessManager | null {
    return processManager;
}

export function getWsBridge(): WebSocketBridge | null {
    return wsBridge;
}

export function cleanup(): void {
    logger.info('Cleaning up preview resources');
    
    if (debounceTimer) {
        clearTimeout(debounceTimer);
        debounceTimer = null;
    }
    
    if (fileWatcher) {
        fileWatcher.dispose();
        fileWatcher = null;
    }
    
    if (processManager) {
        processManager.kill();
        processManager = null;
    }
    
    if (wsBridge) {
        wsBridge.stop();
        wsBridge = null;
    }
    
    messageHandlerRegistered = false;
    isCompiling = false;
}

export async function openPreview(context: vscode.ExtensionContext) {
    logger.show();
    const panel = createWebviewPanel(context);
    logger.info('Preview panel created/revealed');
    
    setOnDispose(() => {
        logger.info('Panel disposed, cleaning up resources');
        cleanup();
    });

    if (!processManager) {
        processManager = new ProcessManager(context);
    }

    if (!messageHandlerRegistered) {
        panel.webview.onDidReceiveMessage(async (message) => {
            logger.debug(`Received message from webview: ${message.type}`);
            
            switch (message.type) {
                case 'refresh':
                    await debouncedRefresh();
                    break;
                case 'runDesktop':
                    await runDesktopInternal();
                    break;
                case 'autoReload':
                    const config = vscode.workspace.getConfiguration('javavisualizer');
                    await config.update('autoReload', message.value, vscode.ConfigurationTarget.Workspace);
                    logger.info(`Auto-reload ${message.value ? 'enabled' : 'disabled'}`);
                    break;
                case 'ready':
                    logger.info('Webview ready');
                    break;
            }
        });
        messageHandlerRegistered = true;
    }

    if (!processManager.isJdkAvailable()) {
        logger.error('JDK not available');
        panel.webview.postMessage({
            type: 'showError',
            title: 'JDK Not Found',
            details: 'Could not find Java Development Kit.\n\nPlease install JDK 17+ and set JAVA_HOME, or configure javavisualizer.javaHome in settings.'
        });
        return;
    }

    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        logger.error('No workspace folder open');
        panel.webview.postMessage({
            type: 'showError',
            title: 'No Workspace',
            details: 'Please open a folder containing a Java project.'
        });
        return;
    }

    if (!wsBridge) {
        wsBridge = new WebSocketBridge();
        try {
            const port = await wsBridge.start();
            logger.info(`WebSocket bridge started on port ${port}`);

            wsBridge.onAgentConnect = () => {
                logger.info('Agent connected to bridge');
                panel.webview.postMessage({ type: 'setStatus', status: 'connected', text: 'Connected' });
                panel.webview.postMessage({ type: 'hideError' });
            };

            wsBridge.onAgentDisconnect = () => {
                logger.info('Agent disconnected from bridge');
                panel.webview.postMessage({ type: 'setStatus', status: '', text: 'Disconnected' });
            };

            wsBridge.onWebviewConnect = () => {
                logger.info('Webview connected to bridge');
            };

            panel.webview.postMessage({ type: 'connectWebSocket', port });
        } catch (error) {
            logger.error('Failed to start WebSocket bridge', error as Error);
            panel.webview.postMessage({
                type: 'showError',
                title: 'WebSocket Error',
                details: `Failed to start WebSocket bridge: ${(error as Error).message}`
            });
            return;
        }
    }

    await compileAndLaunch(workspaceFolder, panel);

    setupFileWatcher(context);

    const settings = getSettings();
    panel.webview.postMessage({ type: 'setAutoReload', value: settings.autoReload });
}

async function compileAndLaunch(workspaceFolder: vscode.WorkspaceFolder, panel: vscode.WebviewPanel) {
    if (isCompiling) {
        logger.warn('Compilation already in progress, skipping');
        return;
    }

    isCompiling = true;
    
    try {
        panel.webview.postMessage({ type: 'setStatus', status: 'compiling', text: 'Compiling...' });

        if (!processManager || !wsBridge) {
            logger.error('ProcessManager or WebSocketBridge not initialized');
            return;
        }

        const project = ProjectDetector.detect(workspaceFolder);
        const wsPort = wsBridge.getPort();
        
        logger.info(`Compiling and launching with agent on port ${wsPort}`);
        const result = await processManager.compileAndRun(project, true, wsPort);

        if (!result.success) {
            logger.error(`Launch failed: ${result.errors.join('\n')}`);
            panel.webview.postMessage({
                type: 'showError',
                title: result.errors.some(e => e.includes('main class')) ? 'Main Class Not Found' : 'Compilation Error',
                details: result.errors.join('\n')
            });
            return;
        }

        logger.info('Launch successful');
        panel.webview.postMessage({ type: 'hideError' });
    } finally {
        isCompiling = false;
    }
}

function setupFileWatcher(context: vscode.ExtensionContext) {
    if (fileWatcher) {
        fileWatcher.dispose();
    }

    fileWatcher = vscode.workspace.createFileSystemWatcher('**/src/**/*.java');

    const onFileChange = () => {
        const settings = getSettings();
        if (!settings.autoReload) {
            return;
        }

        if (debounceTimer) {
            clearTimeout(debounceTimer);
        }

        debounceTimer = setTimeout(async () => {
            logger.info('File change detected, triggering refresh');
            await refreshPreviewInternal();
        }, 500);
    };

    fileWatcher.onDidChange(onFileChange);
    fileWatcher.onDidCreate(onFileChange);
    fileWatcher.onDidDelete(onFileChange);

    context.subscriptions.push(fileWatcher);
}

async function debouncedRefresh() {
    if (isCompiling) {
        logger.warn('Compilation already in progress');
        return;
    }
    await refreshPreviewInternal();
}

async function refreshPreviewInternal() {
    if (!processManager || !wsBridge) {
        logger.warn('Cannot refresh: ProcessManager or WebSocketBridge not available');
        return;
    }

    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        return;
    }

    const panel = getWebviewPanel();
    if (!panel) {
        return;
    }

    await compileAndLaunch(workspaceFolder, panel);
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
    const result = await processManager.compileAndRun(project, false);
    
    if (!result.success) {
        vscode.window.showErrorMessage(`Failed to run: ${result.errors.join('\n')}`);
    } else {
        vscode.window.showInformationMessage('Java application launched on desktop.');
    }
}
