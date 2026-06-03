import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';

export type ProjectType = 'maven' | 'gradle' | 'plain';

export interface ProjectInfo {
    type: ProjectType;
    rootPath: string;
    sourcePath: string;
    outputDir: string;
    classpath: string[];
}

export class ProjectDetector {
    static detect(workspaceFolder: vscode.WorkspaceFolder): ProjectInfo {
        const rootPath = workspaceFolder.uri.fsPath;

        if (fs.existsSync(path.join(rootPath, 'pom.xml'))) {
            return this.detectMaven(rootPath);
        }

        if (fs.existsSync(path.join(rootPath, 'build.gradle')) ||
            fs.existsSync(path.join(rootPath, 'build.gradle.kts'))) {
            return this.detectGradle(rootPath);
        }

        return this.detectPlain(rootPath);
    }

    private static detectMaven(rootPath: string): ProjectInfo {
        const sourcePath = path.join(rootPath, 'src', 'main', 'java');
        const outputDir = path.join(rootPath, 'target', 'classes');
        return {
            type: 'maven',
            rootPath,
            sourcePath,
            outputDir,
            classpath: [],
        };
    }

    private static detectGradle(rootPath: string): ProjectInfo {
        const sourcePath = path.join(rootPath, 'src', 'main', 'java');
        const outputDir = path.join(rootPath, 'build', 'classes', 'java', 'main');
        return {
            type: 'gradle',
            rootPath,
            sourcePath,
            outputDir,
            classpath: [],
        };
    }

    private static detectPlain(rootPath: string): ProjectInfo {
        const sourcePath = fs.existsSync(path.join(rootPath, 'src'))
            ? path.join(rootPath, 'src')
            : rootPath;
        const outputDir = path.join(rootPath, 'bin');
        return {
            type: 'plain',
            rootPath,
            sourcePath,
            outputDir,
            classpath: [],
        };
    }
}
