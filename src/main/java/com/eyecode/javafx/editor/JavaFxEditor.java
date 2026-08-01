package com.eyecode.javafx.editor;

import javafx.scene.layout.BorderPane;
import org.fxmisc.richtext.CodeArea;

public final class JavaFxEditor extends BorderPane {

    private final CodeArea codeArea;

    public JavaFxEditor() {
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area");
        codeArea.setWrapText(true);
        codeArea.useInitialStyleForInsertionProperty().set(true);

        javafx.scene.layout.StackPane wrapper = new javafx.scene.layout.StackPane(codeArea);
        wrapper.getStyleClass().add("editor-wrapper");
        wrapper.setPrefWidth(800);
        wrapper.setPrefHeight(600);

        setCenter(wrapper);
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public String getText() {
        return codeArea.getText();
    }

    public void setText(String text) {
        codeArea.replaceText(text);
    }
}