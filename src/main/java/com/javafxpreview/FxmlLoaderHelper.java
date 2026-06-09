package com.javafxpreview;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class FxmlLoaderHelper {

    public static Parent load(File fxmlFile, ClassLoader classLoader) throws IOException {
        // Try 1: Load normally with custom classloader
        try {
            FXMLLoader loader = new FXMLLoader(fxmlFile.toURI().toURL());
            if (classLoader != null) loader.setClassLoader(classLoader);
            return loader.load();
        } catch (Throwable e) {
            System.out.println("[FxmlLoader] Load with classloader failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // Try 2: Load with default classloader (no custom classpath)
        try {
            FXMLLoader loader = new FXMLLoader(fxmlFile.toURI().toURL());
            return loader.load();
        } catch (Throwable e) {
            System.out.println("[FxmlLoader] Load with default classloader failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }

        // Try 3: Strip controller + event handlers, load pure UI
        try {
            String content = new String(Files.readAllBytes(fxmlFile.toPath()), StandardCharsets.UTF_8);
            content = content.replaceAll("\\s+fx:controller\\s*=\\s*\"[^\"]*\"", "");
            content = content.replaceAll("\\s+fx:define\\s*=\\s*\"[^\"]*\"", "");
            content = content.replaceAll("\\s+on[A-Z]\\w*\\s*=\\s*\"#[^\"]*\"", "");
            System.out.println("[FxmlLoader] Stripped controller + event handlers, loading pure UI...");
            FXMLLoader fallback = new FXMLLoader();
            return fallback.load(new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Throwable e) {
            System.out.println("[FxmlLoader] All attempts failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            throw new IOException("Failed to load FXML: " + e.getMessage(), e);
        }
    }
}
