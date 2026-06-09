package com.javafxpreview.ui;

import javafx.scene.Parent;
import javafx.scene.layout.StackPane;

public class PreviewPane extends StackPane {

    private final ErrorOverlay errorOverlay;

    public PreviewPane(ErrorOverlay errorOverlay) {
        this.errorOverlay = errorOverlay;

        setStyle("-fx-background-color: #2b2b2b;");

        errorOverlay.setViewOrder(-1);
        getChildren().add(errorOverlay);
    }

    public void setContent(Parent root) {
        getChildren().removeIf(n -> n != errorOverlay);
        if (root != null) {
            getChildren().add(0, root);
        }
    }
}
