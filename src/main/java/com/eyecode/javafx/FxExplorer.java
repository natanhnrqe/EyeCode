package com.eyecode.javafx;

import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;

public final class FxExplorer extends StackPane {

    public FxExplorer() {
        getStyleClass().add("explorer");

        Label label = new Label("Project Explorer");
        label.getStyleClass().add("explorer-placeholder");

        getChildren().add(label);
    }
}
