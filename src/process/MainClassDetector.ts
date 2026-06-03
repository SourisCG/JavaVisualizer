import * as fs from 'fs';
import * as path from 'path';
import { ProjectInfo } from '../process/ProjectDetector';
import { logger } from '../utils/logger';

export class MainClassDetector {
    static detect(project: ProjectInfo): string | null {
        logger.info(`Detecting main class for project: ${project.rootPath}`);
        
        const mainClass = this.findMainClassInSource(project.sourcePath);
        if (mainClass) {
            logger.info(`Found main class: ${mainClass}`);
            return mainClass;
        }
        
        logger.warn('No main class found');
        return null;
    }
    
    private static findMainClassInSource(sourcePath: string): string | null {
        if (!fs.existsSync(sourcePath)) {
            logger.warn(`Source path does not exist: ${sourcePath}`);
            return null;
        }
        
        const javaFiles = this.findJavaFiles(sourcePath);
        logger.debug(`Found ${javaFiles.length} Java files to scan`);
        
        for (const file of javaFiles) {
            const mainClass = this.scanJavaFile(file);
            if (mainClass) {
                return mainClass;
            }
        }
        
        return null;
    }
    
    private static findJavaFiles(dir: string): string[] {
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
    
    private static scanJavaFile(filePath: string): string | null {
        try {
            const content = fs.readFileSync(filePath, 'utf-8');
            
            const hasMainMethod = /public\s+static\s+void\s+main\s*\(\s*String\s*\[\s*\]\s+\w+\s*\)/.test(content);
            const hasSpringBoot = /@SpringBootApplication/.test(content);
            const extendsApplication = /extends\s+Application/.test(content);
            
            if (hasMainMethod || hasSpringBoot || extendsApplication) {
                const packageMatch = content.match(/package\s+([\w.]+)\s*;/);
                const classMatch = content.match(/public\s+class\s+(\w+)/);
                
                if (packageMatch && classMatch) {
                    const packageName = packageMatch[1];
                    const className = classMatch[1];
                    const fullClassName = `${packageName}.${className}`;
                    
                    logger.debug(`Found candidate: ${fullClassName} in ${filePath}`);
                    logger.debug(`  - hasMainMethod: ${hasMainMethod}`);
                    logger.debug(`  - hasSpringBoot: ${hasSpringBoot}`);
                    logger.debug(`  - extendsApplication: ${extendsApplication}`);
                    
                    return fullClassName;
                }
            }
        } catch (error) {
            logger.error(`Error scanning file ${filePath}`, error as Error);
        }
        
        return null;
    }
}
