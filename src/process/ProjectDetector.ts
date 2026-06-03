import * as vscode from 'vscode';
import * as path from 'path';
import * as fs from 'fs';
import { logger } from '../utils/logger';

export type ProjectType = 'maven' | 'gradle' | 'plain';

export interface ProjectInfo {
    type: ProjectType;
    rootPath: string;
    sourcePath: string;
    outputDir: string;
    classpath: string[];
    wrapper?: string;
}

export class ProjectDetector {
    static detect(workspaceFolder: vscode.WorkspaceFolder): ProjectInfo {
        const rootPath = workspaceFolder.uri.fsPath;
        logger.info(`Detecting project type in: ${rootPath}`);

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
        const wrapper = this.findMavenWrapper(rootPath);
        
        logger.info(`Detected Maven project at ${rootPath}`);
        logger.info(`  Source path: ${sourcePath}`);
        logger.info(`  Output dir: ${outputDir}`);
        logger.info(`  Wrapper: ${wrapper || 'none'}`);
        
        return {
            type: 'maven',
            rootPath,
            sourcePath,
            outputDir,
            classpath: [],
            wrapper
        };
    }

    private static detectGradle(rootPath: string): ProjectInfo {
        const sourcePath = path.join(rootPath, 'src', 'main', 'java');
        const outputDir = path.join(rootPath, 'build', 'classes', 'java', 'main');
        const wrapper = this.findGradleWrapper(rootPath);
        
        logger.info(`Detected Gradle project at ${rootPath}`);
        logger.info(`  Source path: ${sourcePath}`);
        logger.info(`  Output dir: ${outputDir}`);
        logger.info(`  Wrapper: ${wrapper || 'none'}`);
        
        return {
            type: 'gradle',
            rootPath,
            sourcePath,
            outputDir,
            classpath: [],
            wrapper
        };
    }

    private static detectPlain(rootPath: string): ProjectInfo {
        const sourcePath = fs.existsSync(path.join(rootPath, 'src'))
            ? path.join(rootPath, 'src')
            : rootPath;
        const outputDir = path.join(rootPath, 'bin');
        
        logger.info(`Detected plain Java project at ${rootPath}`);
        logger.info(`  Source path: ${sourcePath}`);
        logger.info(`  Output dir: ${outputDir}`);
        
        return {
            type: 'plain',
            rootPath,
            sourcePath,
            outputDir,
            classpath: [],
        };
    }
    
    private static findMavenWrapper(rootPath: string): string | undefined {
        const mvnw = path.join(rootPath, 'mvnw');
        const mvnwCmd = path.join(rootPath, 'mvnw.cmd');
        
        if (fs.existsSync(mvnw)) {
            try {
                fs.accessSync(mvnw, fs.constants.X_OK);
                return mvnw;
            } catch {
                logger.warn(`mvnw found but not executable: ${mvnw}, falling back to system mvn`);
            }
        }
        if (process.platform === 'win32' && fs.existsSync(mvnwCmd)) {
            return mvnwCmd;
        }
        
        return undefined;
    }
    
    private static findGradleWrapper(rootPath: string): string | undefined {
        const gradlew = path.join(rootPath, 'gradlew');
        const gradlewBat = path.join(rootPath, 'gradlew.bat');
        
        if (fs.existsSync(gradlew)) {
            try {
                fs.accessSync(gradlew, fs.constants.X_OK);
                return gradlew;
            } catch {
                logger.warn(`gradlew found but not executable: ${gradlew}, falling back to system gradle`);
            }
        }
        if (process.platform === 'win32' && fs.existsSync(gradlewBat)) {
            return gradlewBat;
        }
        
        return undefined;
    }
}
