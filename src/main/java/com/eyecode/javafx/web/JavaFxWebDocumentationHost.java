package com.eyecode.javafx.web;

import com.eyecode.javafx.ui.editor.JavaFxDocumentationTab;
import com.eyecode.javafx.ui.editor.JavaFxDocumentationWorkspace;
import com.eyecode.learning.content.DocumentationTarget;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;

public final class JavaFxWebDocumentationHost extends StackPane {
    private final JavaFxWebShellSurface shellSurface;
    private final JavaFxDocumentationWorkspace workspace = new JavaFxDocumentationWorkspace();
    private final JavaFxDocumentationTab tab = workspace.tab();
    private boolean disposed;

    public JavaFxWebDocumentationHost(JavaFxWebShellSurface shellSurface) {
        this.shellSurface = shellSurface;
        getStyleClass().add("web-documentation-host");
        getChildren().add(new BorderPane(tab));
        setVisible(false);
        setManaged(false);
    }

    public void open(DocumentationTarget target) {
        if (disposed || target == null) return;
        Runnable action = () -> {
            if (disposed) return;
            workspace.open(target);
            setVisible(true);
            toFront();
        };
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }

    public void layoutFromBrowser(double x, double y, double width, double height) {
        if (disposed || shellSurface == null || getParent() == null) return;
        Runnable action = () -> {
            if (disposed || getParent() == null) return;
            Point2D sceneMin = shellSurface.localToScene(x, y);
            Point2D sceneMax = shellSurface.localToScene(x + width, y + height);
            Point2D localMin = getParent().sceneToLocal(sceneMin);
            Point2D localMax = getParent().sceneToLocal(sceneMax);
            resizeRelocate(localMin.getX(), localMin.getY(),
                    Math.max(0, localMax.getX() - localMin.getX()),
                    Math.max(0, localMax.getY() - localMin.getY()));
        };
        if (Platform.isFxApplicationThread()) action.run();
        else Platform.runLater(action);
    }

    public void hide() {
        if (disposed) return;
        setVisible(false);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        workspace.dispose();
        getChildren().clear();
    }
}