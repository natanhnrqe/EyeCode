package com.eyecode.javafx.ui.editor;

import javafx.scene.Node;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class FxEditorContentPane extends StackPane {

    public FxEditorContentPane() {
        getStyleClass().add("editor-content-pane");
        setMinSize(0, 0);
    }

    public void show(Node node) {
        getChildren().setAll(node);
        StackPane.setAlignment(node, Pos.TOP_LEFT);
        if (node instanceof Region region) {
            region.setMinSize(0, 0);
            region.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        }
    }

    Node mountedContentForTest() {
        return getChildren().isEmpty() ? null : getChildren().getFirst();
    }
}
