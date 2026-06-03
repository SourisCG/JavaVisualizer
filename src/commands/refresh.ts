import * as vscode from 'vscode';
import { getWebviewPanel } from '../webview/WebviewProvider';
import { getProcessManager, getWsBridge } from './preview';
import { ProjectDetector } from '../process/ProjectDetector';
import { logger } from '../utils/logger';

export function setContext(context: vscode.ExtensionContext) {
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

    const processManager = getProcessManager();
    const wsBridge = getWsBridge();

    if (!processManager) {
        vscode.window.showErrorMessage('ProcessManager not available. Please open the preview first.');
        return;
    }

    if (!wsBridge) {
        vscode.window.showErrorMessage('WebSocket bridge not available. Please open the preview first.');
        return;
    }

    logger.info('Refreshing preview from command palette');
    panel.webview.postMessage({ type: 'setStatus', status: 'compiling', text: 'Compiling...' });

    const project = ProjectDetector.detect(workspaceFolder);
    const wsPort = wsBridge.getPort();
    const result = await processManager.compileAndRun(project, true, wsPort);

    if (!result.success) {
        logger.error(`Refresh failed: ${result.errors.join('\n')}`);
        panel.webview.postMessage({
            type: 'showError',
            title: 'Compilation Error',
            details: result.errors.join('\n')
        });
        return;
    }

    logger.info('Refresh successful');
    panel.webview.postMessage({ type: 'hideError' });
    panel.webview.postMessage({ type: 'connectWebSocket', port: wsPort });
}
