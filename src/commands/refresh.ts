import * as vscode from 'vscode';
import { getWebviewPanel } from '../webview/WebviewProvider';
import { ProcessManager } from '../process/ProcessManager';
import { ProjectDetector } from '../process/ProjectDetector';

let extensionContext: vscode.ExtensionContext | null = null;

export function setContext(context: vscode.ExtensionContext) {
    extensionContext = context;
}

export async function refreshPreview() {
    const panel = getWebviewPanel();
    if (!panel) {
        vscode.window.showWarningMessage('JavaVisualizer preview is not open.');
        return;
    }

    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        vscode.window.showErrorMessage('No workspace folder open.');
        return;
    }

    if (!extensionContext) {
        vscode.window.showErrorMessage('Extension context not available.');
        return;
    }

    panel.webview.postMessage({ type: 'setStatus', status: 'compiling', text: 'Compiling...' });

    const processManager = new ProcessManager(extensionContext);
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
}
