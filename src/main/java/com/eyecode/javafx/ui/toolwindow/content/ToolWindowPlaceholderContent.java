package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.javafx.designsystem.FxSpacing;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.VBox;

public abstract class ToolWindowPlaceholderContent extends ScrollPane {

    private final VBox body;

    protected ToolWindowPlaceholderContent(String title) {
        getStyleClass().add("toolwindow-scroll");
        setFitToWidth(true);
        setFitToHeight(true);

        body = new VBox();
        body.getStyleClass().add("toolwindow-content");
        body.setSpacing(FxSpacing.LG);

        Label heading = new Label(title);
        heading.getStyleClass().add("toolwindow-heading");
        body.getChildren().add(heading);

        setContent(body);
    }

    protected void addSection(String title, Node... children) {
        body.getChildren().add(new FxToolWindowSection(title, children));
    }

    protected Label placeholder(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("toolwindow-placeholder");
        return l;
    }
}