package com.eyecode.javafx.editor;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import org.fxmisc.flowless.VirtualizedScrollPane;
import org.fxmisc.richtext.CodeArea;

public final class JavaFxEditor extends HBox {

    private final CodeArea codeArea;
    private final VirtualizedScrollPane<CodeArea> scrollPane;

    public JavaFxEditor() {
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-area");
        codeArea.setWrapText(false);
        codeArea.useInitialStyleForInsertionProperty().set(true);

        codeArea.setParagraphGraphicFactory(new JavaFxGutterFactory(codeArea));

        scrollPane = new VirtualizedScrollPane<>(codeArea);
        scrollPane.getStyleClass().add("editor-scroll-pane");
        HBox.setHgrow(scrollPane, Priority.ALWAYS);

        getStyleClass().add("editor-root");
        setMaxWidth(Double.MAX_VALUE);
        setMaxHeight(Double.MAX_VALUE);

        getChildren().add(scrollPane);
    }

    public CodeArea getCodeArea() {
        return codeArea;
    }

    public VirtualizedScrollPane<CodeArea> getScrollPane() {
        return scrollPane;
    }

    public String getText() {
        return codeArea.getText();
    }

    public void setText(String text) {
        codeArea.replaceText(text);
    }
}