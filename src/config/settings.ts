import * as vscode from 'vscode';

export interface ExtensionSettings {
    autoReload: boolean;
    frameRate: number;
    jpegQuality: number;
    javaHome: string;
    mainClass: string;
}

export function getSettings(): ExtensionSettings {
    const config = vscode.workspace.getConfiguration('javavisualizer');
    return {
        autoReload: config.get<boolean>('autoReload', false),
        frameRate: config.get<number>('frameRate', 30),
        jpegQuality: config.get<number>('jpegQuality', 75),
        javaHome: config.get<string>('javaHome', ''),
        mainClass: config.get<string>('mainClass', ''),
    };
}
