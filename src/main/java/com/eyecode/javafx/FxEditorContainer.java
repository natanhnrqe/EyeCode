package com.eyecode.javafx;

import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextArea;
import javafx.scene.layout.StackPane;

public final class FxEditorContainer extends TabPane {

    public FxEditorContainer() {
        getStyleClass().add("editor-container");

        Tab tab = new Tab();
        tab.setText("Untitled");
        tab.setClosable(true);
        tab.setContent(buildPlaceholderContent());
        getTabs().add(tab);
    }

    private StackPane buildPlaceholderContent() {
        StackPane pane = new StackPane();
        pane.getStyleClass().add("editor-area");

        TextArea area = new TextArea();
        area.setEditable(false);
        area.setWrapText(true);
        area.getStyleClass().add("editor-placeholder");
        area.setText("// Editor Area placeholder\n// Sprint 1 will integrate EditorDocument + EditorBuffer");

        pane.getChildren().add(area);
        return pane;
    }
}
