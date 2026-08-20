package com.eyecode.javafx.editor;

import javafx.beans.value.ChangeListener;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import org.fxmisc.richtext.CodeArea;

import java.util.function.IntFunction;

public final class JavaFxGutterFactory implements IntFunction<Node> {

    static final double GUTTER_WIDTH = 52;

    private final CodeArea codeArea;

    public JavaFxGutterFactory(CodeArea codeArea) {
        this.codeArea = codeArea;
    }

    @Override
    public Node apply(int lineIndex) {
        Label label = new Label(Integer.toString(lineIndex + 1));
        label.getStyleClass().add("lineno");
        label.setAlignment(Pos.CENTER_LEFT);
        label.setPrefWidth(GUTTER_WIDTH);
        label.setMinWidth(GUTTER_WIDTH);
        label.setMaxWidth(GUTTER_WIDTH);
        label.setPadding(new Insets(0, 6, 0, 8));

        if (lineIndex == codeArea.getCurrentParagraph()) {
            label.getStyleClass().add("lineno-current");
        }

        ChangeListener<Number> listener = (obs, old, current) -> {
            boolean wasCurrent = old != null && old.intValue() == lineIndex;
            boolean isCurrent = current != null && current.intValue() == lineIndex;
            if (wasCurrent == isCurrent) {
                return;
            }
            label.getStyleClass().remove("lineno-current");
            if (isCurrent) {
                label.getStyleClass().add("lineno-current");
            }
        };
        codeArea.currentParagraphProperty().addListener(listener);
        return label;
    }
}
