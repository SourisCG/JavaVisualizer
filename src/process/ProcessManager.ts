import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { spawn, ChildProcess, execSync } from 'child_process';
import { ProjectInfo } from './ProjectDetector';
import { JavaCompiler } from './JavaCompiler';
import { MainClassDetector } from './MainClassDetector';
import { getSettings } from '../config/settings';
import { logger } from '../utils/logger';

export class ProcessManager {
    private javaProcess: ChildProcess | null = null;
    private compiler: JavaCompiler;
    private agentJarPath: string;

    constructor(private context: vscode.ExtensionContext) {
        this.compiler = new JavaCompiler();
        this.agentJarPath = path.join(context.extensionUri.fsPath, 'resources', 'JavaVisualizerAgent.jar');
        logger.info(`ProcessManager initialized, agent JAR: ${this.agentJarPath}`);
    }

    async compileAndRun(project: ProjectInfo, withAgent: boolean = true, wsPort?: number): Promise<{ success: boolean; errors: string[] }> {
        this.kill();

        logger.info(`Compiling project: ${project.rootPath}`);
        const compileResult = await this.compiler.compile(project);
        if (!compileResult.success) {
            logger.error('Compilation failed', new Error(compileResult.errors.join('\n')));
            return compileResult;
        }
        logger.info('Compilation successful');

        if (withAgent) {
            return this.runWithAgent(project, wsPort);
        } else {
            return this.runDesktop(project);
        }
    }

    private async runWithAgent(project: ProjectInfo, wsPort?: number): Promise<{ success: boolean; errors: string[] }> {
        const settings = getSettings();
        const javaExe = this.compiler.getJavaExecutable();
        
        const mainClass = settings.mainClass || MainClassDetector.detect(project);
        if (!mainClass) {
            return { success: false, errors: ['Could not detect main class. Please set javavisualizer.mainClass in settings.'] };
        }

        const classpath = project.classpath.length > 0
            ? project.classpath.join(path.delimiter)
            : project.outputDir;
        
        const args = [
            `-javaagent:${this.agentJarPath}=${wsPort || 9876}`,
            '-cp', classpath,
            mainClass
        ];

        const fullCommand = `${javaExe} ${args.join(' ')}`;
        logger.info(`Launching Java process with agent`);
        logger.info(`  Java: ${javaExe}`);
        logger.info(`  Agent JAR: ${this.agentJarPath}`);
        logger.info(`  Agent JAR exists: ${fs.existsSync(this.agentJarPath)}`);
        logger.info(`  Main class: ${mainClass}`);
        logger.info(`  Classpath entries: ${classpath.split(path.delimiter).length}`);
        logger.info(`  WebSocket port: ${wsPort || 9876}`);
        logger.info(`  FULL COMMAND: ${fullCommand}`);
        logger.info(`  Working directory: ${project.rootPath}`);

        return new Promise((resolve) => {
            this.javaProcess = spawn(javaExe, args, {
                cwd: project.rootPath,
                detached: true,
                stdio: ['ignore', 'pipe', 'pipe']
            });

            let stderr = '';
            let stdout = '';
            let resolved = false;

            this.javaProcess.stdout?.on('data', (data) => {
                stdout += data.toString();
                logger.debug(`[Java stdout] ${data.toString().trim()}`);
            });

            this.javaProcess.stderr?.on('data', (data) => {
                stderr += data.toString();
                logger.debug(`[Java stderr] ${data.toString().trim()}`);
            });

            this.javaProcess.on('error', (err) => {
                logger.error('Java process spawn error', err);
                if (!resolved) {
                    resolved = true;
                    resolve({ success: false, errors: [err.message] });
                }
            });

            this.javaProcess.on('exit', (code) => {
                logger.info(`Java process exited with code: ${code}`);
                this.javaProcess = null;
                
                if (!resolved && code !== 0) {
                    resolved = true;
                    const errors = stderr.split('\n').filter(line => line.trim());
                    resolve({ success: false, errors });
                }
            });

            setTimeout(() => {
                if (!resolved) {
                    resolved = true;
                    if (this.javaProcess && !this.javaProcess.killed) {
                        logger.info('Java process started successfully (verified after 3s)');
                        resolve({ success: true, errors: [] });
                    } else {
                        const errors = stderr.split('\n').filter(line => line.trim());
                        resolve({ success: false, errors: errors.length > 0 ? errors : ['Java process exited unexpectedly'] });
                    }
                }
            }, 3000);
        });
    }

    async runDesktop(project: ProjectInfo): Promise<{ success: boolean; errors: string[] }> {
        const settings = getSettings();
        const javaExe = this.compiler.getJavaExecutable();
        
        const mainClass = settings.mainClass || MainClassDetector.detect(project);
        if (!mainClass) {
            return { success: false, errors: ['Could not detect main class. Please set javavisualizer.mainClass in settings.'] };
        }

        const classpath = project.classpath.length > 0
            ? project.classpath.join(path.delimiter)
            : project.outputDir;

        const args = [
            '-cp', classpath,
            mainClass
        ];

        logger.info(`Launching Java process on desktop`);
        logger.info(`  Java: ${javaExe}`);
        logger.info(`  Main class: ${mainClass}`);

        return new Promise((resolve) => {
            this.javaProcess = spawn(javaExe, args, {
                cwd: project.rootPath,
                detached: true
            });

            this.javaProcess.on('error', (err) => {
                logger.error('Java desktop process spawn error', err);
                resolve({ success: false, errors: [err.message] });
            });

            this.javaProcess.unref();
            
            setTimeout(() => {
                resolve({ success: true, errors: [] });
            }, 1000);
        });
    }

    kill(): void {
        if (this.javaProcess) {
            logger.info(`Killing Java process (PID: ${this.javaProcess.pid})`);
            
            try {
                if (process.platform === 'win32') {
                    execSync(`taskkill /pid ${this.javaProcess.pid} /T /F`, { stdio: 'ignore' });
                } else {
                    if (this.javaProcess.pid) {
                        process.kill(-this.javaProcess.pid, 'SIGTERM');
                    }
                }
            } catch (error) {
                logger.warn(`Failed to kill process group, trying direct kill`);
                try {
                    this.javaProcess.kill('SIGKILL');
                } catch (killError) {
                    logger.error('Failed to kill Java process', killError as Error);
                }
            }
            this.javaProcess = null;
            logger.info('Java process killed');
        }
    }

    isRunning(): boolean {
        return this.javaProcess !== null && !this.javaProcess.killed;
    }

    isJdkAvailable(): boolean {
        return this.compiler.isJdkAvailable();
    }
}
