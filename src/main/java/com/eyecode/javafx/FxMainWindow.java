package com.eyecode.javafx;

import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class FxMainWindow {

    private static final String TITLE = "EyeCode";
    private static final double WIDTH = 1200;
    private static final double HEIGHT = 800;
    private static final double MIN_WIDTH = 1000;
    private static final double MIN_HEIGHT = 600;

    private final Stage stage;
    private final FxRootLayout root;

    public FxMainWindow(Stage stage) {
        this.stage = stage;
        this.root = new FxRootLayout(() -> {
            Platform.exit();
            System.exit(0);
        });

        stage.initStyle(StageStyle.UNDECORATED);
        stage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
        stage.setTitle(TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);

        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());

        Scene scene = new Scene(root);
        scene.getStylesheets().add(
                getClass().getResource("/javafx/style/eyecode.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() {
        stage.show();
        Region r = root;
        r.requestLayout();
    }
}
