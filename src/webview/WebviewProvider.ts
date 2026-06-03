import * as vscode from 'vscode';
import { getWebviewContent } from './getWebviewContent';

let currentPanel: vscode.WebviewPanel | null = null;

export function getWebviewPanel(): vscode.WebviewPanel | null {
    return currentPanel;
}

export function createWebviewPanel(context: vscode.ExtensionContext): vscode.WebviewPanel {
    if (currentPanel) {
        currentPanel.reveal(vscode.ViewColumn.Beside);
        return currentPanel;
    }

    const panel = vscode.window.createWebviewPanel(
        'javavisualizer',
        'JavaVisualizer',
        vscode.ViewColumn.Beside,
        {
            enableScripts: true,
            retainContextWhenHidden: true,
            localResourceRoots: [
                vscode.Uri.joinPath(context.extensionUri, 'resources')
            ]
        }
    );

    panel.webview.html = getWebviewContent(panel.webview);

    panel.onDidDispose(() => {
        currentPanel = null;
    });

    currentPanel = panel;
    return panel;
}
