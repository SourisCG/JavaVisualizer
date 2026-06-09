package com.javafxpreview;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import com.javafxpreview.core.FileWatcher;
import com.javafxpreview.core.ViewportManager;

import java.io.File;
import java.net.URL;
import java.util.*;

public class App extends Application {

    private Stage stage;
    private Scene scene;
    private BorderPane root;

    // UI controls
    private ComboBox<String> fxmlCombo;
    private ComboBox<ViewportManager.Preset> viewportCombo;
    private TextField customW;
    private TextField customH;
    private Circle statusDot;
    private Label statusLabel;
    private StackPane previewArea;
    private Label errorLabel;
    private StackPane errorOverlay;

    // State
    private File currentProjectRoot;
    private File currentFxml;
    private List<File> fxmlFiles = new ArrayList<>();
    private List<File> cssFiles = new ArrayList<>();
    private List<String> cssPaths = new ArrayList<>();
    private FileWatcher watcher;
    private boolean autoReload = true;
    private String lastDir = System.getProperty("user.home");
    private ViewportManager viewportManager;

    @Override
    public void start(Stage primaryStage) {
        this.stage = primaryStage;
        stage.setTitle("JavaFX Live Preview");
        stage.setMinWidth(400);
        stage.setMinHeight(300);
        stage.setWidth(1100);
        stage.setHeight(768);

        viewportManager = new ViewportManager(stage);

        buildUI();

        String fxmlPath = null;
        Parameters params = getParameters();
        if (!params.getRaw().isEmpty()) {
            fxmlPath = params.getRaw().get(0);
        }

        if (fxmlPath != null) {
            openFile(new File(fxmlPath));
        }
        stage.show();
    }

    private void buildUI() {
        // Toolbar
        Button openBtn = new Button("Open...");
        openBtn.setStyle("-fx-font-size: 11px;");
        openBtn.setOnAction(e -> chooseFile());

        fxmlCombo = new ComboBox<>();
        fxmlCombo.setPromptText("-- Select FXML --");
        fxmlCombo.setPrefWidth(200);
        fxmlCombo.setStyle("-fx-font-size: 11px;");
        fxmlCombo.setOnAction(e -> onFxmlSelected());

        ToggleButton autoBtn = new ToggleButton("Auto");
        autoBtn.setStyle("-fx-font-size: 11px;");
        autoBtn.setSelected(true);
        autoBtn.setOnAction(e -> autoReload = autoBtn.isSelected());

        viewportCombo = new ComboBox<>();
        viewportCombo.getItems().addAll(ViewportManager.Preset.values());
        viewportCombo.setValue(ViewportManager.Preset.NATIVE);
        viewportCombo.setPrefWidth(160);
        viewportCombo.setStyle("-fx-font-size: 11px;");
        viewportCombo.setOnAction(e -> applyViewport());

        customW = new TextField("600");
        customW.setPrefWidth(48);
        customW.setStyle("-fx-font-size: 11px;");
        customH = new TextField("400");
        customH.setPrefWidth(48);
        customH.setStyle("-fx-font-size: 11px;");
        customW.setVisible(false);
        customH.setVisible(false);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        statusDot = new Circle(4, Color.GRAY);
        statusLabel = new Label("Ready");
        statusLabel.setTextFill(Color.GRAY);
        statusLabel.setStyle("-fx-font-size: 11px;");
        HBox statusBox = new HBox(5, statusDot, statusLabel);
        statusBox.setAlignment(Pos.CENTER);

        HBox toolbar = new HBox(5, openBtn, fxmlCombo, autoBtn, viewportCombo, customW, customH, spacer, statusBox);
        toolbar.setPadding(new Insets(4, 8, 4, 8));
        toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setStyle("-fx-background-color: #3c3c3c; -fx-border-color: #555; -fx-border-width: 0 0 1 0;");

        // Preview area
        previewArea = new StackPane();
        previewArea.setStyle("-fx-background-color: #2b2b2b;");

        // Error overlay (on top of preview)
        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-family: monospace;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(700);
        errorLabel.setAlignment(Pos.CENTER);

        errorOverlay = new StackPane(errorLabel);
        errorOverlay.setStyle("-fx-background-color: rgba(180,20,20,0.85);");
        errorOverlay.setPadding(new Insets(20));
        errorOverlay.setVisible(false);
        errorOverlay.setViewOrder(-1);

        previewArea.getChildren().add(errorOverlay);

        // Layout
        root = new BorderPane();
        root.setTop(toolbar);
        root.setCenter(previewArea);

        scene = new Scene(root, 1100, 768);
        stage.setScene(scene);
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open FXML File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));
        File dir = new File(lastDir);
        if (dir.exists() && dir.isDirectory()) {
            chooser.setInitialDirectory(dir);
        }
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            lastDir = file.getParent();
            openFile(file);
        }
    }

    private void openFile(File fxmlFile) {
        if (!fxmlFile.exists()) {
            showError("File not found: " + fxmlFile.getPath());
            return;
        }

        setStatus("Loading...");

        try {
            currentProjectRoot = detectProjectRoot(fxmlFile);

            fxmlFiles.clear();
            scanFiles(currentProjectRoot, fxmlFiles, ".fxml");
            updateFxmlCombo(fxmlFile.getName());

            cssFiles.clear();
            scanFiles(currentProjectRoot, cssFiles, ".css");

            currentFxml = fxmlFile;
            loadCurrentFxml();

            applyAllCss();

            startWatcher();

            setStatusOk(fxmlFile.getName());

            Platform.runLater(() -> {
                stage.sizeToScene();
                viewportManager.saveNativeSize();
            });

        } catch (Throwable t) {
            t.printStackTrace();
            showError("Error: " + t.getMessage());
        }
    }

    private void loadCurrentFxml() {
        if (currentFxml == null) return;
        try {
            Parent loaded = FxmlLoaderHelper.load(currentFxml, buildClassLoader());
            if (loaded != null) {
                previewArea.getChildren().removeIf(n -> n != errorOverlay);
                previewArea.getChildren().add(0, loaded);
            }
        } catch (Throwable t) {
            t.printStackTrace();
            showError("FXML error: " + t.getMessage());
        }
    }

    private void applyAllCss() {
        // Clear scene stylesheets
        scene.getStylesheets().clear();
        cssPaths.clear();

        // Add all CSS files
        for (File css : cssFiles) {
            try {
                String url = css.toURI().toURL().toExternalForm();
                cssPaths.add(url);
                scene.getStylesheets().add(url);
            } catch (Exception ignored) {}
        }

        // Force CSS reapply
        if (root != null) {
            root.applyCss();
            root.requestLayout();
        }
    }

    private ClassLoader buildClassLoader() {
        List<File> dirs = new ArrayList<>();
        String[] paths = {
            "target/classes",
            "build/classes/java/main",
            "build/classes/main",
            "build/resources/main",
            "out/production/classes",
            "bin"
        };
        for (String p : paths) {
            File d = new File(currentProjectRoot, p);
            if (d.exists()) dirs.add(d);
        }
        if (dirs.isEmpty()) dirs.add(currentProjectRoot);

        List<URL> urls = new ArrayList<>();
        for (File dir : dirs) {
            try { urls.add(dir.toURI().toURL()); } catch (Exception ignored) {}
        }
        if (urls.isEmpty()) return getClass().getClassLoader();
        return new java.net.URLClassLoader(urls.toArray(new URL[0]), getClass().getClassLoader());
    }

    private void startWatcher() {
        if (watcher != null) watcher.stop();
        watcher = new FileWatcher(
            path -> Platform.runLater(() -> {
                System.out.println("[Watcher] FXML changed: " + path);
                currentFxml = path.toFile();
                loadCurrentFxml();
                setStatusOk(currentFxml.getName());
            }),
            path -> Platform.runLater(() -> {
                System.out.println("[Watcher] CSS changed: " + path);
                cssFiles.clear();
                scanFiles(currentProjectRoot, cssFiles, ".css");
                applyAllCss();
            })
        );
        watcher.setAutoReload(autoReload);
        watcher.watch(currentProjectRoot, currentFxml, cssFiles);
    }

    private void onFxmlSelected() {
        String selected = fxmlCombo.getValue();
        if (selected == null) return;
        Map<String, String> nameToPath = (Map<String, String>) fxmlCombo.getUserData();
        if (nameToPath == null) return;
        String path = nameToPath.get(selected);
        if (path == null) return;

        File fxmlFile = new File(path);
        if (!fxmlFile.exists()) return;

        currentFxml = fxmlFile;
        loadCurrentFxml();
        applyAllCss();
        setStatusOk(fxmlFile.getName());
        hideError();
        fxmlCombo.setValue(selected);
    }

    private void applyViewport() {
        ViewportManager.Preset preset = viewportCombo.getValue();
        boolean isCustom = preset == ViewportManager.Preset.CUSTOM;
        customW.setVisible(isCustom);
        customH.setVisible(isCustom);
        if (isCustom) {
            int w = 0;
            try { w = Integer.parseInt(customW.getText()); } catch (NumberFormatException ignored) {}
            viewportManager.setPreset(preset, w, 0);
        } else {
            viewportManager.setPreset(preset);
        }
    }

    private void updateFxmlCombo(String currentName) {
        Map<String, String> nameToPath = new HashMap<>();
        List<String> names = new ArrayList<>();
        for (File f : fxmlFiles) {
            names.add(f.getName());
            nameToPath.put(f.getName(), f.getAbsolutePath());
        }
        fxmlCombo.getItems().clear();
        fxmlCombo.getItems().addAll(names);
        fxmlCombo.setUserData(nameToPath);
        fxmlCombo.setValue(currentName);
    }

    private void scanFiles(File dir, List<File> result, String extension) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            String name = f.getName();
            if (f.isDirectory() && !name.startsWith(".") && !name.equals("target") && !name.equals("build")) {
                scanFiles(f, result, extension);
            } else if (name.toLowerCase().endsWith(extension)) {
                result.add(f);
            }
        }
    }

    private File detectProjectRoot(File fxmlFile) {
        File dir = fxmlFile.getParentFile();
        while (dir != null) {
            File[] children = dir.listFiles();
            if (children != null) {
                for (File child : children) {
                    String n = child.getName().toLowerCase();
                    if (n.equals("src") && child.isDirectory()) return dir;
                    if (n.equals("pom.xml")) return dir;
                    if (n.equals("build.gradle") || n.equals("build.gradle.kts")) return dir;
                }
            }
            dir = dir.getParentFile();
        }
        return fxmlFile.getParentFile();
    }

    private void setStatus(String text) {
        statusDot.setFill(Color.GRAY);
        statusLabel.setText(text);
        statusLabel.setTextFill(Color.GRAY);
    }

    private void setStatusOk(String text) {
        statusDot.setFill(Color.LIMEGREEN);
        statusLabel.setText(text);
        statusLabel.setTextFill(Color.LIMEGREEN);
    }

    private void showError(String msg) {
        errorLabel.setText(msg);
        errorOverlay.setVisible(true);
        statusDot.setFill(Color.RED);
        statusLabel.setText("Error");
        statusLabel.setTextFill(Color.RED);
    }

    private void hideError() {
        errorOverlay.setVisible(false);
    }
}
