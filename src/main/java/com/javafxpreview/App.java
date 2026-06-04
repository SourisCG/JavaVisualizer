package com.javafxpreview;

import javafx.application.Application;
import javafx.stage.Stage;
import com.javafxpreview.ui.MainWindow;

public class App extends Application {

    @Override
    public void start(Stage primaryStage) {
        MainWindow window = new MainWindow(primaryStage);

        String fxmlPath = null;
        Parameters params = getParameters();
        if (params.getRaw().size() > 0) {
            fxmlPath = params.getRaw().get(0);
        }

        window.show(fxmlPath);
    }
}
