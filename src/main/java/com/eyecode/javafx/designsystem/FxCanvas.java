package com.eyecode.javafx.designsystem;

import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class FxCanvas extends StackPane {

    public FxCanvas() {
        getStyleClass().add("canvas-root");
    }

    public FxCanvas(Region content) {
        this();
        setContent(content);
    }

    public void setContent(Region content) {
        getChildren().setAll(content);
    }
}
