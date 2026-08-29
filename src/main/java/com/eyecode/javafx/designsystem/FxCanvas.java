package com.eyecode.javafx.designsystem;

import javafx.scene.Node;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class FxCanvas extends StackPane {

    private Node overlay;

    public FxCanvas() {
        getStyleClass().add("canvas-root");
    }

    public FxCanvas(Region content) {
        this();
        setContent(content);
    }

    public void setContent(Region content) {
        getChildren().setAll(content);
        if (overlay != null) getChildren().add(overlay);
    }

    public void setOverlay(Node overlay) {
        if (this.overlay != null) getChildren().remove(this.overlay);
        this.overlay = overlay;
        if (overlay != null) {
            getChildren().add(overlay);
            StackPane.setAlignment(overlay, javafx.geometry.Pos.TOP_LEFT);
        }
    }
}
