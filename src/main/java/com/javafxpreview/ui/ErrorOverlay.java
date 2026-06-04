package com.javafxpreview.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public class ErrorOverlay extends StackPane {

    private final Label errorLabel;

    public ErrorOverlay() {
        setStyle("-fx-background-color: rgba(180,20,20,0.85);");
        setVisible(false);
        setPadding(new Insets(20));

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: white; -fx-font-size: 14px; -fx-font-family: monospace;");
        errorLabel.setWrapText(true);
        errorLabel.setMaxWidth(700);
        errorLabel.setAlignment(Pos.CENTER);

        getChildren().add(errorLabel);
    }

    public void show(String message) {
        errorLabel.setText(message);
        setVisible(true);
    }

    public void hide() {
        setVisible(false);
    }
}
