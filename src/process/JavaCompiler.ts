import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { spawn, spawnSync, ChildProcess } from 'child_process';
import { ProjectInfo } from './ProjectDetector';
import { getSettings } from '../config/settings';
import { logger } from '../utils/logger';

export class JavaCompiler {
    private javaHome: string;

    constructor() {
        this.javaHome = this.resolveJavaHome();
        logger.info(`JavaCompiler initialized with JAVA_HOME: ${this.javaHome || '(not set)'}`);
    }

    private resolveJavaHome(): string {
        const settings = getSettings();
        if (settings.javaHome) {
            logger.info(`Using configured javaHome: ${settings.javaHome}`);
            return settings.javaHome;
        }
        const envJavaHome = process.env.JAVA_HOME;
        if (envJavaHome) {
            logger.info(`Using JAVA_HOME from environment: ${envJavaHome}`);
            return envJavaHome;
        }
        logger.warn('No JAVA_HOME configured');
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
        const available = this.javaHome !== '' || this.isCommandAvailable('java');
        logger.info(`JDK available: ${available}`);
        return available;
    }

    private isCommandAvailable(command: string): boolean {
        try {
            logger.debug(`Checking if command is available: ${command}`);
            const result = spawnSync(command, ['--version'], { 
                shell: true,
                timeout: 5000,
                stdio: 'ignore'
            });
            
            const available = result.status === 0;
            logger.debug(`Command '${command}' available: ${available}`);
            return available;
        } catch (error) {
            logger.debug(`Command '${command}' not available: ${(error as Error).message}`);
            return false;
        }
    }

    async compile(project: ProjectInfo): Promise<{ success: boolean; errors: string[] }> {
        logger.info(`Compiling project type: ${project.type}`);
        
        switch (project.type) {
            case 'maven':
                return this.compileMaven(project);
            case 'gradle':
                return this.compileGradle(project);
            case 'plain':
                return this.compilePlain(project);
            default:
                return { success: false, errors: [`Unknown project type: ${project.type}`] };
        }
    }

    private async compileMaven(project: ProjectInfo): Promise<{ success: boolean; errors: string[] }> {
        const command = project.wrapper || 'mvn';
        logger.info(`Using Maven command: ${command}`);
        
        const compileResult = await this.runCommand(command, ['compile'], project.rootPath);
        if (!compileResult.success) {
            return compileResult;
        }
        
        const classpathResult = await this.getMavenClasspath(project);
        if (classpathResult.success && classpathResult.classpath) {
            project.classpath = classpathResult.classpath;
            logger.info(`Maven classpath resolved: ${project.classpath.length} entries`);
        } else {
            logger.warn('Failed to resolve Maven classpath');
        }
        
        return compileResult;
    }

    private async compileGradle(project: ProjectInfo): Promise<{ success: boolean; errors: string[] }> {
        const command = project.wrapper || 'gradle';
        logger.info(`Using Gradle command: ${command}`);
        
        const compileResult = await this.runCommand(command, ['compileJava'], project.rootPath);
        if (!compileResult.success) {
            return compileResult;
        }
        
        const classpathResult = await this.getGradleClasspath(project);
        if (classpathResult.success && classpathResult.classpath) {
            project.classpath = classpathResult.classpath;
            logger.info(`Gradle classpath resolved: ${project.classpath.length} entries`);
        } else {
            logger.warn('Failed to resolve Gradle classpath');
        }
        
        return compileResult;
    }

    private async compilePlain(project: ProjectInfo): Promise<{ success: boolean; errors: string[] }> {
        if (!fs.existsSync(project.outputDir)) {
            try {
                fs.mkdirSync(project.outputDir, { recursive: true });
                logger.info(`Created output directory: ${project.outputDir}`);
            } catch (error) {
                logger.error(`Failed to create output directory: ${project.outputDir}`, error as Error);
                return { success: false, errors: [`Failed to create output directory: ${(error as Error).message}`] };
            }
        }

        const javaFiles = this.findJavaFiles(project.sourcePath);
        if (javaFiles.length === 0) {
            return { success: false, errors: ['No .java files found'] };
        }

        logger.info(`Compiling ${javaFiles.length} Java files`);

        const args = [
            '-d', project.outputDir,
            '-cp', project.classpath.join(path.delimiter),
            ...javaFiles
        ];

        return this.runCommand(this.getJavacExecutable(), args, project.rootPath);
    }

    private async getMavenClasspath(project: ProjectInfo): Promise<{ success: boolean; classpath?: string[]; errors: string[] }> {
        const command = project.wrapper || 'mvn';
        const tempFile = path.join(project.rootPath, '.classpath.txt');
        
        logger.info(`Getting Maven classpath using: ${command}`);
        
        const result = await this.runCommand(
            command,
            ['dependency:build-classpath', `-Dmdep.outputFile=${tempFile}`, '-q'],
            project.rootPath
        );
        
        if (!result.success) {
            return result;
        }
        
        try {
            if (fs.existsSync(tempFile)) {
                const classpathContent = fs.readFileSync(tempFile, 'utf-8').trim();
                fs.unlinkSync(tempFile);
                
                const classpath = classpathContent.split(path.delimiter).filter(p => p.length > 0);
                classpath.push(project.outputDir);
                
                return { success: true, classpath, errors: [] };
            }
        } catch (error) {
            logger.error('Error reading Maven classpath file', error as Error);
        }
        
        return { success: false, errors: ['Failed to read Maven classpath'] };
    }

    private async getGradleClasspath(project: ProjectInfo): Promise<{ success: boolean; classpath?: string[]; errors: string[] }> {
        const command = project.wrapper || 'gradle';
        
        logger.info(`Getting Gradle classpath using: ${command}`);
        
        const result = await this.runCommand(
            command,
            ['-q', 'printClasspath'],
            project.rootPath
        );
        
        if (result.success && result.stdout) {
            const classpath = result.stdout.trim().split(path.delimiter).filter(p => p.length > 0);
            classpath.push(project.outputDir);
            return { success: true, classpath, errors: [] };
        }
        
        logger.warn('Gradle printClasspath task not found, using output directory only');
        return { success: true, classpath: [project.outputDir], errors: [] };
    }

    private findJavaFiles(dir: string): string[] {
        const files: string[] = [];
        
        try {
            const entries = fs.readdirSync(dir, { withFileTypes: true });

            for (const entry of entries) {
                const fullPath = path.join(dir, entry.name);
                if (entry.isDirectory()) {
                    files.push(...this.findJavaFiles(fullPath));
                } else if (entry.name.endsWith('.java')) {
                    files.push(fullPath);
                }
            }
        } catch (error) {
            logger.error(`Error reading directory ${dir}`, error as Error);
        }

        return files;
    }

    private runCommand(
        command: string,
        args: string[],
        cwd: string,
        timeout: number = 120000
    ): Promise<{ success: boolean; errors: string[]; stdout?: string }> {
        return new Promise((resolve) => {
            logger.debug(`Running: ${command} ${args.join(' ')}`);
            logger.debug(`  in directory: ${cwd}`);
            
            const proc = spawn(command, args, { cwd, shell: true });
            let stderr = '';
            let stdout = '';
            let timedOut = false;
            
            const timeoutId = setTimeout(() => {
                timedOut = true;
                proc.kill('SIGTERM');
                logger.error(`Command timed out after ${timeout}ms: ${command}`);
            }, timeout);

            proc.stdout.on('data', (data) => {
                stdout += data.toString();
            });

            proc.stderr.on('data', (data) => {
                stderr += data.toString();
            });

            proc.on('close', (code) => {
                clearTimeout(timeoutId);
                
                if (timedOut) {
                    resolve({ success: false, errors: [`Command timed out after ${timeout}ms`] });
                } else if (code === 0) {
                    logger.debug(`Command completed successfully`);
                    resolve({ success: true, errors: [], stdout });
                } else {
                    logger.error(`Command failed with code ${code}`);
                    logger.error(`stderr: ${stderr}`);
                    const errors = stderr.split('\n').filter(line => line.trim());
                    resolve({ success: false, errors, stdout });
                }
            });

            proc.on('error', (err) => {
                clearTimeout(timeoutId);
                logger.error(`Command error: ${err.message}`, err);
                resolve({ success: false, errors: [err.message] });
            });
        });
    }
}
