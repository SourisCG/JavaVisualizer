package com.javafxpreview.core;

import javafx.scene.Scene;

public class ViewportManager {

    public enum Preset {
        NATIVE(0, 0, "Native"),
        PHONE(375, 667, "Phone (375×667)"),
        TABLET(768, 1024, "Tablet (768×1024)"),
        DESKTOP(1024, 768, "Desktop (1024×768)"),
        HD(1920, 1080, "HD (1920×1080)"),
        CUSTOM(0, 0, "Custom");

        public final int width;
        public final int height;
        public final String label;

        Preset(int width, int height, String label) {
            this.width = width;
            this.height = height;
            this.label = label;
        }

        @Override
        public String toString() { return label; }
    }

    private Preset current = Preset.NATIVE;
    private int customWidth = 600;
    private int customHeight = 400;
    private double nativeWidth;
    private double nativeHeight;
    private final javafx.stage.Stage stage;

    public ViewportManager(javafx.stage.Stage stage) {
        this.stage = stage;
    }

    public Preset getCurrent() { return current; }

    public void setPreset(Preset preset) {
        setPreset(preset, customWidth, customHeight);
    }

    public void setPreset(Preset preset, int customW, int customH) {
        this.current = preset;
        this.customWidth = customW;
        this.customHeight = customH;

        if (preset == Preset.NATIVE) {
            stage.setWidth(nativeWidth);
            stage.setHeight(nativeHeight);
            stage.setResizable(true);
            stage.sizeToScene();
        } else {
            stage.setResizable(false);
            int w = preset == Preset.CUSTOM ? customW : preset.width;
            int h = preset == Preset.CUSTOM ? customH : preset.height;
            stage.setWidth(w);
            stage.setHeight(h);
        }
    }

    public void saveNativeSize() {
        nativeWidth = stage.getWidth();
        nativeHeight = stage.getHeight();
    }

    public int getCustomWidth() { return customWidth; }
    public int getCustomHeight() { return customHeight; }
}
