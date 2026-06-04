package com.javafxpreview.ui;

import javafx.scene.Scene;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import com.javafxpreview.config.AppSettings;
import com.javafxpreview.core.FxmlWatcher;
import com.javafxpreview.core.HotReloadService;
import com.javafxpreview.core.ViewportManager;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class MainWindow {

    private final Stage stage;
    private final Toolbar toolbar;
    private final ErrorOverlay errorOverlay;
    private final ViewportManager viewportManager;
    private HotReloadService reloadService;
    private FxmlWatcher watcher;
    private File projectRoot;
    private List<File> fxmlFiles = new ArrayList<>();

    public MainWindow(Stage stage) {
        this.stage = stage;
        this.stage.setTitle("JavaFX Live Preview");
        this.stage.setMinWidth(400);
        this.stage.setMinHeight(300);
        this.stage.setWidth(1100);
        this.stage.setHeight(768);

        viewportManager = new ViewportManager(stage);

        toolbar = new Toolbar();
        errorOverlay = new ErrorOverlay();

        Scene scene = new Scene(new BorderPane(), 1100, 768);

        PreviewPane previewPane = new PreviewPane(errorOverlay);

        reloadService = new HotReloadService(
            scene,
            root -> previewPane.setContent(root),
            error -> {
                toolbar.setStatusError(error);
                errorOverlay.show(error);
            },
            warning -> toolbar.setStatus(warning),
            () -> {
                toolbar.setStatusOk("OK");
                errorOverlay.hide();
            }
        );

        BorderPane root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(previewPane);
        scene.setRoot(root);

        stage.setScene(scene);

        toolbar.setOnOpenFile(this::openFile);
        toolbar.setOnFxmlSelect(this::switchFxml);
        toolbar.setOnViewportChange((preset, customW) -> {
            if (preset == ViewportManager.Preset.CUSTOM) {
                viewportManager.setPreset(preset, customW, 0);
            } else {
                viewportManager.setPreset(preset);
            }
        });
        toolbar.setOnAutoReloadChange(enabled -> {
            AppSettings.setAutoReload(enabled);
            if (watcher != null) watcher.setAutoReload(enabled);
        });
    }

    public void show(String fxmlPath) {
        if (fxmlPath != null) {
            openFile(new File(fxmlPath));
        }
        stage.show();
    }

    private void openFile(File fxmlFile) {
        if (!fxmlFile.exists()) {
            toolbar.setStatusError("File not found: " + fxmlFile.getPath());
            return;
        }

        toolbar.setStatus("Loading...");

        try {
            projectRoot = detectProjectRoot(fxmlFile);

            // Scan all FXML files
            fxmlFiles.clear();
            scanFxmlFiles(projectRoot, fxmlFiles);
            updateFxmlList(fxmlFile.getName());

            // Detect classpath
            File[] classpathDirs = detectClasspath(projectRoot);
            reloadService.setClasspathDirs(classpathDirs);

            reloadService.loadFxml(fxmlFile);
            toolbar.setStatusOk(fxmlFile.getName());

            // CSS files
            reloadService.clearStylesheets();
            List<File> cssFileList = findCssFiles(projectRoot);
            cssFileList.forEach(reloadService::addStylesheet);

            // Watcher
            if (watcher != null) watcher.stop();
            watcher = new FxmlWatcher(
                path -> {
                    System.out.println("[Watcher] FXML change: " + path);
                    reloadService.onFxmlChanged(path);
                    javafx.application.Platform.runLater(() -> {
                        scanFxmlFiles(projectRoot, fxmlFiles);
                        updateFxmlList(fxmlFile.getName());
                    });
                },
                path -> {
                    System.out.println("[Watcher] CSS change: " + path);
                    reloadService.onCssChanged(path);
                }
            );
            watcher.setAutoReload(toolbar.getAutoReloadBtn().isSelected());
            watcher.setCurrentFxml(fxmlFile);
            watcher.setCssFiles(cssFileList);
            watcher.watch(projectRoot);

            errorOverlay.hide();

            javafx.application.Platform.runLater(() -> {
                stage.sizeToScene();
                viewportManager.saveNativeSize();
            });

        } catch (Exception e) {
            e.printStackTrace();
            toolbar.setStatusError(e.getMessage());
            errorOverlay.show("Error loading FXML:\n" + e.getMessage()
                + "\n\nClasspath: " + Arrays.toString(detectClasspath(projectRoot)));
        }
    }

    private void switchFxml(File fxmlFile) {
        if (!fxmlFile.exists()) return;
        toolbar.setStatus("Loading...");
        try {
            reloadService.loadFxml(fxmlFile);
            toolbar.setStatusOk(fxmlFile.getName());
            toolbar.selectFxml(fxmlFile.getName());
            errorOverlay.hide();
        } catch (Exception e) {
            e.printStackTrace();
            toolbar.setStatusError(e.getMessage());
            errorOverlay.show("Error: " + e.getMessage());
        }
    }

    private void updateFxmlList(String currentName) {
        List<String> names = new ArrayList<>();
        Map<String, String> nameToPath = new HashMap<>();
        for (File f : fxmlFiles) {
            String name = f.getName();
            names.add(name);
            nameToPath.put(name, f.getAbsolutePath());
        }
        toolbar.setFxmlList(names, nameToPath);
        toolbar.selectFxml(currentName);
    }

    private void scanFxmlFiles(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory() && !name.startsWith(".") && !name.equals("target") && !name.equals("build")) {
                scanFxmlFiles(f, result);
            } else if (name.endsWith(".fxml")) {
                result.add(f);
            }
        }
    }

    private File detectProjectRoot(File fxmlFile) {
        File dir = fxmlFile.getParentFile();
        while (dir != null) {
            if (new File(dir, "src").exists()) return dir;
            if (new File(dir, "pom.xml").exists()) return dir;
            if (new File(dir, "build.gradle").exists()) return dir;
            dir = dir.getParentFile();
        }
        return fxmlFile.getParentFile();
    }

    private File[] detectClasspath(File projectRoot) {
        List<File> dirs = new ArrayList<>();
        File mavenClasses = new File(projectRoot, "target/classes");
        if (mavenClasses.exists()) dirs.add(mavenClasses);
        File gradleClasses = new File(projectRoot, "build/classes/java/main");
        if (gradleClasses.exists()) dirs.add(gradleClasses);
        File gradleResources = new File(projectRoot, "build/resources/main");
        if (gradleResources.exists()) dirs.add(gradleResources);
        File plainBin = new File(projectRoot, "bin");
        if (plainBin.exists()) dirs.add(plainBin);
        if (dirs.isEmpty()) dirs.add(projectRoot);
        return dirs.toArray(new File[0]);
    }

    private List<File> findCssFiles(File root) {
        List<File> cssFiles = new ArrayList<>();
        findCssRecursive(root, cssFiles);
        return cssFiles;
    }

    private void findCssRecursive(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory() && !name.startsWith(".") && !name.equals("target") && !name.equals("build")) {
                findCssRecursive(f, result);
            } else if (name.endsWith(".css")) {
                result.add(f);
            }
        }
    }
}
