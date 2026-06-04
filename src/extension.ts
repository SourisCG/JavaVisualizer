import * as vscode from 'vscode';
import { openPreview, cleanup as cleanupPreview, getWsBridge } from './commands/preview';
import { refreshPreview, setContext as setRefreshContext } from './commands/refresh';
import { runDesktop, setContext as setRunDesktopContext } from './commands/runDesktop';
import { initLogger, logger } from './utils/logger';

export function activate(context: vscode.ExtensionContext) {
    initLogger(context);
    logger.info('JavaVisualizer extension activating');
    
    setRefreshContext(context);
    setRunDesktopContext(context);

    const openPreviewCmd = vscode.commands.registerCommand(
        'javavisualizer.openPreview',
        () => {
            logger.info('Command: openPreview');
            openPreview(context);
        }
    );

    const refreshCmd = vscode.commands.registerCommand(
        'javavisualizer.refresh',
        () => {
            logger.info('Command: refresh');
            refreshPreview();
        }
    );

    const runDesktopCmd = vscode.commands.registerCommand(
        'javavisualizer.runDesktop',
        () => {
            logger.info('Command: runDesktop');
            runDesktop();
        }
    );

    const configListener = vscode.workspace.onDidChangeConfiguration((e) => {
        if (e.affectsConfiguration('javavisualizer')) {
            logger.info('Configuration changed, forwarding to agent');
            const config = vscode.workspace.getConfiguration('javavisualizer');
            const wsBridge = getWsBridge();
            if (wsBridge && wsBridge.isAgentConnected()) {
                wsBridge.sendEvent({
                    type: 'config',
                    frameRate: config.get<number>('frameRate', 30),
                    jpegQuality: config.get<number>('jpegQuality', 75) / 100
                });
            }
        }
    });

    context.subscriptions.push(openPreviewCmd, refreshCmd, runDesktopCmd, configListener);
    logger.info('JavaVisualizer extension activated');
}

export function deactivate() {
    logger.info('JavaVisualizer extension deactivating');
    cleanupPreview();
    logger.info('JavaVisualizer extension deactivated');
}
