package com.javafxpreview.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import com.javafxpreview.config.AppSettings;
import com.javafxpreview.core.ViewportManager;

import java.io.File;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class Toolbar extends HBox {

    private final Circle statusDot;
    private final Label statusLabel;
    private final ComboBox<String> fxmlCombo;
    private final ComboBox<ViewportManager.Preset> viewportCombo;
    private final TextField customW;
    private final TextField customH;
    private final ToggleButton autoReloadBtn;

    private Consumer<File> onOpenFile;
    private Consumer<File> onFxmlSelect;
    private BiConsumer<ViewportManager.Preset, Integer> onViewportChange;
    private Consumer<Boolean> onAutoReloadChange;

    public Toolbar() {
        setPadding(new Insets(4, 8, 4, 8));
        setSpacing(5);
        setAlignment(Pos.CENTER_LEFT);
        setStyle("-fx-background-color: #3c3c3c; -fx-border-color: #555; -fx-border-width: 0 0 1 0;");

        Button openBtn = new Button("Open...");
        openBtn.setStyle("-fx-font-size: 11px;");
        openBtn.setOnAction(e -> chooseFile());

        fxmlCombo = new ComboBox<>();
        fxmlCombo.setPromptText("-- Select FXML --");
        fxmlCombo.setPrefWidth(200);
        fxmlCombo.setStyle("-fx-font-size: 11px;");
        fxmlCombo.setOnAction(e -> {
            String selected = fxmlCombo.getValue();
            if (selected != null && onFxmlSelect != null) {
                @SuppressWarnings("unchecked")
                String filePath = fxmlCombo.getUserData() instanceof java.util.Map
                    ? ((java.util.Map<String, String>) fxmlCombo.getUserData()).get(selected)
                    : null;
                if (filePath != null) {
                    onFxmlSelect.accept(new File(filePath));
                }
            }
        });

        autoReloadBtn = new ToggleButton("Auto");
        autoReloadBtn.setStyle("-fx-font-size: 11px;");
        autoReloadBtn.setSelected(AppSettings.isAutoReload());
        autoReloadBtn.setOnAction(e -> {
            if (onAutoReloadChange != null) onAutoReloadChange.accept(autoReloadBtn.isSelected());
        });

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

        getChildren().addAll(openBtn, fxmlCombo, autoReloadBtn, viewportCombo, customW, customH, spacer, statusBox);
    }

    private void chooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Open FXML File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("FXML Files", "*.fxml"));
        File dir = new File(AppSettings.getLastDirectory());
        if (dir.exists() && dir.isDirectory()) {
            chooser.setInitialDirectory(dir);
        }
        File file = chooser.showOpenDialog(getScene().getWindow());
        if (file != null) {
            AppSettings.setLastDirectory(file.getParent());
            if (onOpenFile != null) onOpenFile.accept(file);
        }
    }

    private void applyViewport() {
        ViewportManager.Preset preset = viewportCombo.getValue();
        boolean isCustom = preset == ViewportManager.Preset.CUSTOM;
        customW.setVisible(isCustom);
        customH.setVisible(isCustom);
        if (onViewportChange != null) {
            int w = 0, h = 0;
            try { w = Integer.parseInt(customW.getText()); } catch (NumberFormatException ignored) {}
            try { h = Integer.parseInt(customH.getText()); } catch (NumberFormatException ignored) {}
            onViewportChange.accept(preset, isCustom ? w : 0);
        }
    }

    public void setFxmlList(List<String> names, java.util.Map<String, String> nameToPath) {
        fxmlCombo.getItems().clear();
        fxmlCombo.getItems().addAll(names);
        fxmlCombo.setUserData(nameToPath);
    }

    public void selectFxml(String name) {
        if (name != null) fxmlCombo.setValue(name);
    }

    public void setStatusOk(String text) {
        statusDot.setFill(Color.LIMEGREEN);
        statusLabel.setText(text);
        statusLabel.setTextFill(Color.LIMEGREEN);
    }

    public void setStatusError(String text) {
        statusDot.setFill(Color.RED);
        statusLabel.setText(text);
        statusLabel.setTextFill(Color.RED);
    }

    public void setStatus(String text) {
        statusDot.setFill(Color.GRAY);
        statusLabel.setText(text);
        statusLabel.setTextFill(Color.GRAY);
    }

    public void setOnOpenFile(Consumer<File> handler) { this.onOpenFile = handler; }
    public void setOnFxmlSelect(Consumer<File> handler) { this.onFxmlSelect = handler; }
    public void setOnViewportChange(BiConsumer<ViewportManager.Preset, Integer> handler) { this.onViewportChange = handler; }
    public void setOnAutoReloadChange(Consumer<Boolean> handler) { this.onAutoReloadChange = handler; }
    public ToggleButton getAutoReloadBtn() { return autoReloadBtn; }
    public ComboBox<ViewportManager.Preset> getViewportCombo() { return viewportCombo; }
}
