package com.eyecode.javafx.ui;

import com.eyecode.javafx.ceffx.CeffxRuntime;
import com.eyecode.javafx.web.JavaFxWebShellSurface;
import com.eyecode.javafx.web.WebShellNativeController;
import com.eyecode.javafx.web.WebShellMode;
import com.eyecode.javafx.web.WebShellWorkspaceController;
import javafx.application.Platform;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public final class FxMainWindow {
    private static final String TITLE = "EyeCode";
    private static final double MIN_WIDTH = 1000;
    private static final double MIN_HEIGHT = 600;
    private final Stage stage;
    private final Region root;
    private final WebShellWorkspaceController webShellWorkspace;
    private final JavaFxWebShellSurface webShellSurface;

    public FxMainWindow(Stage stage) {
        this.stage = stage;
        if (WebShellMode.configured() == WebShellMode.WEB_SHELL) {
            webShellSurface = new JavaFxWebShellSurface();
            webShellWorkspace = new WebShellWorkspaceController(webShellSurface);
            new WebShellNativeController(webShellSurface, stage);
            root = new StackPane(webShellSurface);
        } else {
            webShellSurface = null;
            webShellWorkspace = null;
            root = new FxRootLayout(this::shutdown);
        }
        stage.initStyle(StageStyle.UNDECORATED);
        stage.setOnCloseRequest(e -> shutdown());
        stage.setTitle(TITLE);
        stage.setMinWidth(MIN_WIDTH);
        stage.setMinHeight(MIN_HEIGHT);
        Rectangle2D screenBounds = Screen.getPrimary().getVisualBounds();
        stage.setWidth(screenBounds.getWidth());
        stage.setHeight(screenBounds.getHeight());
        stage.setX(screenBounds.getMinX());
        stage.setY(screenBounds.getMinY());
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/javafx/style/eyecode.css").toExternalForm());
        stage.setScene(scene);
    }

    public void show() { stage.show(); root.requestLayout(); }

    private void shutdown() {
        if (root instanceof FxRootLayout legacyRoot) legacyRoot.dispose();
        if (webShellWorkspace != null) webShellWorkspace.dispose();
        if (webShellSurface != null) webShellSurface.dispose();
        CeffxRuntime.dispose();
        Platform.exit();
        System.exit(0);
    }
}


