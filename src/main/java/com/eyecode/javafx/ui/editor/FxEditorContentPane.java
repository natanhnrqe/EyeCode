package com.eyecode.javafx.ui.editor;

import javafx.scene.Node;
import javafx.scene.layout.StackPane;

public final class FxEditorContentPane extends StackPane {

    public FxEditorContentPane() {
        getStyleClass().add("editor-content-pane");
        setMinSize(0, 0);
    }

    public void show(Node node) {
        getChildren().setAll(node);
    }

    Node mountedContentForTest() {
        return getChildren().isEmpty() ? null : getChildren().getFirst();
    }
}
