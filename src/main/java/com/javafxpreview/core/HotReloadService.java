package com.javafxpreview.core;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class HotReloadService {

    private final Scene scene;
    private final Consumer<Parent> onRootLoaded;
    private final Consumer<String> onError;
    private final Consumer<String> onWarning;
    private final Runnable onSuccess;
    private File currentFxml;
    private final List<String> cssPaths = new ArrayList<>();
    private URLClassLoader projectClassLoader;
    private File[] classpathDirs = new File[0];

    public HotReloadService(Scene scene, Consumer<Parent> onRootLoaded,
                            Consumer<String> onError, Consumer<String> onWarning, Runnable onSuccess) {
        this.scene = scene;
        this.onRootLoaded = onRootLoaded;
        this.onError = onError;
        this.onWarning = onWarning;
        this.onSuccess = onSuccess;
    }

    public void setClasspathDirs(File[] dirs) {
        this.classpathDirs = dirs;
        refreshClassLoader();
    }

    private void refreshClassLoader() {
        try { if (projectClassLoader != null) projectClassLoader.close(); } catch (IOException ignored) {}
        List<URL> urls = new ArrayList<>();
        for (File dir : classpathDirs) {
            if (dir.exists() && dir.isDirectory()) {
                try { urls.add(dir.toURI().toURL()); } catch (MalformedURLException ignored) {}
            }
        }
        projectClassLoader = !urls.isEmpty()
            ? new URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader())
            : null;
    }

    public void loadFxml(File fxmlFile) throws IOException {
        currentFxml = fxmlFile;
        cssPaths.clear();
        reloadFxml();
    }

    public void onFxmlChangedDirect(Path path) {
        if (currentFxml == null) return;
        File changed = path.toFile();
        if (changed.getName().toLowerCase().endsWith(".fxml")) currentFxml = changed;
        refreshClassLoader();
        try { reloadFxml(); onSuccess.run(); }
        catch (Exception e) { e.printStackTrace(); onError.accept("FXML reload error: " + e.getMessage()); }
    }

    public void onCssChanged(Path path) {
        Platform.runLater(() -> {
            try { reloadCss(); onSuccess.run(); }
            catch (Exception e) { e.printStackTrace(); onError.accept("CSS reload error: " + e.getMessage()); }
        });
    }

    private void reloadFxml() throws IOException {
        if (currentFxml == null) return;
        FXMLLoader loader = new FXMLLoader(currentFxml.toURI().toURL());
        if (projectClassLoader != null) loader.setClassLoader(projectClassLoader);
        try {
            Parent root = loader.load();
            onRootLoaded.accept(root);
            reloadCss();
        } catch (Exception e) {
            e.printStackTrace();
            if (e instanceof ClassNotFoundException || e.getCause() instanceof ClassNotFoundException) {
                String msg = e.getMessage() != null ? e.getMessage() : String.valueOf(e.getCause());
                onWarning.accept("No controller: " + msg + " — UI only mode");
                FXMLLoader fallbackLoader = new FXMLLoader(currentFxml.toURI().toURL());
                if (projectClassLoader != null) fallbackLoader.setClassLoader(projectClassLoader);
                fallbackLoader.setControllerFactory(classId -> null);
                try {
                    Parent root = fallbackLoader.load();
                    onRootLoaded.accept(root);
                    reloadCss();
                } catch (Exception ex) {
                    ex.printStackTrace();
                    onError.accept("Failed to load FXML: " + ex.getMessage());
                }
                return;
            }
            throw e;
        }
    }

    private void reloadCss() throws MalformedURLException {
        List<String> urls = new ArrayList<>();
        urls.add("Modena");
        for (String path : cssPaths) {
            urls.add(new File(path).toURI().toURL().toExternalForm());
        }
        scene.getStylesheets().setAll(urls);
        if (scene.getRoot() != null) {
            scene.getRoot().applyCss();
            scene.getRoot().requestLayout();
        }
    }

    public void addStylesheet(File cssFile) {
        cssPaths.add(cssFile.getAbsolutePath());
        try { scene.getStylesheets().add(cssFile.toURI().toURL().toExternalForm()); } catch (MalformedURLException ignored) {}
    }

    public void clearStylesheets() { cssPaths.clear(); scene.getStylesheets().setAll("Modena"); }
    public File getCurrentFxml() { return currentFxml; }
}
