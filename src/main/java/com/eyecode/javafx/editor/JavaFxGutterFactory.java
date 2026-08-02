package com.eyecode.javafx.editor;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.fxmisc.richtext.CodeArea;

import java.util.function.IntFunction;

public final class JavaFxGutterFactory implements IntFunction<Node> {

    private final CodeArea codeArea;

    public JavaFxGutterFactory(CodeArea codeArea) {
        this.codeArea = codeArea;
    }

    @Override
    public Node apply(int lineIndex) {

        Label label = new Label(Integer.toString(lineIndex + 1));

        label.getStyleClass().add("lineno");

        if (lineIndex == codeArea.getCurrentParagraph()) {
            label.getStyleClass().add("lineno-current");
        }

        label.setAlignment(Pos.CENTER_RIGHT);

        label.setPrefWidth(48);
        label.setMinWidth(48);
        label.setMaxWidth(48);

        label.setPadding(new Insets(0, 10, 0, 6));

        return label;
    }
}