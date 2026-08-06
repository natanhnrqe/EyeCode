package com.eyecode.javafx.ui.toolwindow.content;

import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class FxToolWindowSection extends VBox {

    public FxToolWindowSection(String title, Node... children) {
        getStyleClass().add("toolwindow-section");
        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("toolwindow-section-title");
        getChildren().add(titleLabel);
        getChildren().addAll(children);
    }
}