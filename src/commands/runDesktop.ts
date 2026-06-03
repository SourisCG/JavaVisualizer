import * as vscode from 'vscode';
import { getProcessManager } from './preview';
import { ProcessManager } from '../process/ProcessManager';
import { ProjectDetector } from '../process/ProjectDetector';
import { logger } from '../utils/logger';

let extensionContext: vscode.ExtensionContext | null = null;

export function setContext(context: vscode.ExtensionContext) {
    extensionContext = context;
}

export async function runDesktop() {
    const workspaceFolder = vscode.workspace.workspaceFolders?.[0];
    if (!workspaceFolder) {
        vscode.window.showErrorMessage('No workspace folder open.');
        return;
    }

    let processManager = getProcessManager();
    
    if (!processManager && extensionContext) {
        processManager = new ProcessManager(extensionContext);
    }

    if (!processManager) {
        vscode.window.showErrorMessage('ProcessManager not available.');
        return;
    }

    if (!processManager.isJdkAvailable()) {
        vscode.window.showErrorMessage(
            'JDK not found. Please install JDK 17+ and set JAVA_HOME, or configure javavisualizer.javaHome in settings.'
        );
        return;
    }

    const project = ProjectDetector.detect(workspaceFolder);

    logger.info('Running project on desktop');

    vscode.window.withProgress(
        {
            location: vscode.ProgressLocation.Notification,
            title: 'JavaVisualizer: Compiling...',
            cancellable: false
        },
        async (progress) => {
            progress.report({ increment: 0 });

            const compileResult = await processManager!.compileAndRun(project, false);

            if (!compileResult.success) {
                logger.error(`Desktop run failed: ${compileResult.errors.join('\n')}`);
                vscode.window.showErrorMessage(`Compilation failed:\n${compileResult.errors.join('\n')}`);
                return;
            }

            progress.report({ increment: 100 });
            logger.info('Desktop launch successful');
            vscode.window.showInformationMessage('Java application launched on desktop.');
        }
    );
}
