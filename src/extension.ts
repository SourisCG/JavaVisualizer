import * as vscode from 'vscode';
import { openPreview, cleanup as cleanupPreview } from './commands/preview';
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

    context.subscriptions.push(openPreviewCmd, refreshCmd, runDesktopCmd);
    logger.info('JavaVisualizer extension activated');
}

export function deactivate() {
    logger.info('JavaVisualizer extension deactivating');
    cleanupPreview();
    logger.info('JavaVisualizer extension deactivated');
}
