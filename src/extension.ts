import * as vscode from 'vscode';
import { openPreview } from './commands/preview';
import { refreshPreview, setContext as setRefreshContext } from './commands/refresh';
import { runDesktop, setContext as setRunDesktopContext } from './commands/runDesktop';

export function activate(context: vscode.ExtensionContext) {
    setRefreshContext(context);
    setRunDesktopContext(context);

    const openPreviewCmd = vscode.commands.registerCommand(
        'javavisualizer.openPreview',
        () => openPreview(context)
    );

    const refreshCmd = vscode.commands.registerCommand(
        'javavisualizer.refresh',
        () => refreshPreview()
    );

    const runDesktopCmd = vscode.commands.registerCommand(
        'javavisualizer.runDesktop',
        () => runDesktop()
    );

    context.subscriptions.push(openPreviewCmd, refreshCmd, runDesktopCmd);
}

export function deactivate() {}
