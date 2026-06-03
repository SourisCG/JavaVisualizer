import * as vscode from 'vscode';
import * as path from 'path';
import { spawn, ChildProcess } from 'child_process';
import { ProjectInfo } from './ProjectDetector';
import { JavaCompiler } from './JavaCompiler';
import { getSettings } from '../config/settings';

export class ProcessManager {
    private javaProcess: ChildProcess | null = null;
    private compiler: JavaCompiler;
    private agentJarPath: string;

    constructor(private context: vscode.ExtensionContext) {
        this.compiler = new JavaCompiler();
        this.agentJarPath = path.join(context.extensionPath, 'resources', 'JavaVisualizerAgent.jar');
    }

    async compileAndRun(project: ProjectInfo, withAgent: boolean = true): Promise<{ success: boolean; errors: string[] }> {
        this.kill();

        const compileResult = await this.compiler.compile(project);
        if (!compileResult.success) {
            return compileResult;
        }

        if (withAgent) {
            return this.runWithAgent(project);
        } else {
            return this.runDesktop(project);
        }
    }

    private runWithAgent(project: ProjectInfo): { success: boolean; errors: string[] } {
        const settings = getSettings();
        const javaExe = this.compiler.getJavaExecutable();

        const args = [
            `-javaagent:${this.agentJarPath}`,
            '-cp', project.outputDir + path.delimiter + project.classpath.join(path.delimiter),
            settings.mainClass || this.detectMainClass(project)
        ];

        this.javaProcess = spawn(javaExe, args, {
            cwd: project.rootPath,
            shell: true
        });

        this.javaProcess.on('error', (err) => {
            vscode.window.showErrorMessage(`Failed to start Java process: ${err.message}`);
        });

        this.javaProcess.on('exit', (code) => {
            this.javaProcess = null;
        });

        return { success: true, errors: [] };
    }

    runDesktop(project: ProjectInfo): { success: boolean; errors: string[] } {
        const settings = getSettings();
        const javaExe = this.compiler.getJavaExecutable();

        const args = [
            '-cp', project.outputDir + path.delimiter + project.classpath.join(path.delimiter),
            settings.mainClass || this.detectMainClass(project)
        ];

        this.javaProcess = spawn(javaExe, args, {
            cwd: project.rootPath,
            shell: true,
            detached: true
        });

        this.javaProcess.unref();

        return { success: true, errors: [] };
    }

    private detectMainClass(project: ProjectInfo): string {
        return '';
    }

    kill(): void {
        if (this.javaProcess) {
            try {
                if (this.javaProcess.pid) {
                    process.kill(-this.javaProcess.pid, 'SIGTERM');
                }
            } catch {
                try {
                    this.javaProcess.kill('SIGKILL');
                } catch {}
            }
            this.javaProcess = null;
        }
    }

    isRunning(): boolean {
        return this.javaProcess !== null && !this.javaProcess.killed;
    }

    isJdkAvailable(): boolean {
        return this.compiler.isJdkAvailable();
    }
}
