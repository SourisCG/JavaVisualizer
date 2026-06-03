import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { spawn, ChildProcess } from 'child_process';
import { ProjectInfo } from './ProjectDetector';
import { getSettings } from '../config/settings';

export class JavaCompiler {
    private javaHome: string;

    constructor() {
        this.javaHome = this.resolveJavaHome();
    }

    private resolveJavaHome(): string {
        const settings = getSettings();
        if (settings.javaHome) {
            return settings.javaHome;
        }
        const envJavaHome = process.env.JAVA_HOME;
        if (envJavaHome) {
            return envJavaHome;
        }
        return '';
    }

    getJavaExecutable(): string {
        if (!this.javaHome) {
            return 'java';
        }
        return path.join(this.javaHome, 'bin', 'java');
    }

    getJavacExecutable(): string {
        if (!this.javaHome) {
            return 'javac';
        }
        return path.join(this.javaHome, 'bin', 'javac');
    }

    isJdkAvailable(): boolean {
        return this.javaHome !== '' || this.isCommandAvailable('java');
    }

    private isCommandAvailable(command: string): boolean {
        try {
            const result = spawn(command, ['--version']);
            return true;
        } catch {
            return false;
        }
    }

    async compile(project: ProjectInfo): Promise<{ success: boolean; errors: string[] }> {
        switch (project.type) {
            case 'maven':
                return this.compileMaven(project);
            case 'gradle':
                return this.compileGradle(project);
            case 'plain':
                return this.compilePlain(project);
        }
    }

    private async compileMaven(project: ProjectInfo): Promise<{ success: boolean; errors: string[] }> {
        return this.runCommand('mvn', ['compile'], project.rootPath);
    }

    private async compileGradle(project: ProjectInfo): Promise<{ success: boolean; errors: string[] }> {
        return this.runCommand('gradle', ['compileJava'], project.rootPath);
    }

    private async compilePlain(project: ProjectInfo): Promise<{ success: boolean; errors: string[] }> {
        if (!fs.existsSync(project.outputDir)) {
            fs.mkdirSync(project.outputDir, { recursive: true });
        }

        const javaFiles = this.findJavaFiles(project.sourcePath);
        if (javaFiles.length === 0) {
            return { success: false, errors: ['No .java files found'] };
        }

        const args = [
            '-d', project.outputDir,
            '-cp', project.classpath.join(path.delimiter),
            ...javaFiles
        ];

        return this.runCommand(this.getJavacExecutable(), args, project.rootPath);
    }

    private findJavaFiles(dir: string): string[] {
        const files: string[] = [];
        const entries = fs.readdirSync(dir, { withFileTypes: true });

        for (const entry of entries) {
            const fullPath = path.join(dir, entry.name);
            if (entry.isDirectory()) {
                files.push(...this.findJavaFiles(fullPath));
            } else if (entry.name.endsWith('.java')) {
                files.push(fullPath);
            }
        }

        return files;
    }

    private runCommand(command: string, args: string[], cwd: string): Promise<{ success: boolean; errors: string[] }> {
        return new Promise((resolve) => {
            const proc = spawn(command, args, { cwd, shell: true });
            let stderr = '';

            proc.stderr.on('data', (data) => {
                stderr += data.toString();
            });

            proc.on('close', (code) => {
                if (code === 0) {
                    resolve({ success: true, errors: [] });
                } else {
                    const errors = stderr.split('\n').filter(line => line.trim());
                    resolve({ success: false, errors });
                }
            });

            proc.on('error', (err) => {
                resolve({ success: false, errors: [err.message] });
            });
        });
    }
}
