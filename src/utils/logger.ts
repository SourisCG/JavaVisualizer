import * as vscode from 'vscode';

let outputChannel: vscode.OutputChannel | null = null;

export function initLogger(context: vscode.ExtensionContext) {
    outputChannel = vscode.window.createOutputChannel('JavaVisualizer');
    context.subscriptions.push(outputChannel);
}

export const logger = {
    info: (message: string) => {
        const timestamp = new Date().toISOString();
        outputChannel?.appendLine(`[${timestamp}] [INFO] ${message}`);
    },
    
    warn: (message: string) => {
        const timestamp = new Date().toISOString();
        outputChannel?.appendLine(`[${timestamp}] [WARN] ${message}`);
    },
    
    error: (message: string, error?: Error) => {
        const timestamp = new Date().toISOString();
        outputChannel?.appendLine(`[${timestamp}] [ERROR] ${message}`);
        if (error?.stack) {
            outputChannel?.appendLine(error.stack);
        }
        outputChannel?.show(true);
    },
    
    debug: (message: string) => {
        const timestamp = new Date().toISOString();
        outputChannel?.appendLine(`[${timestamp}] [DEBUG] ${message}`);
    },
    
    show: () => {
        outputChannel?.show();
    }
};
