package com.eyecode.javafx.ui;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public final class FxExplorer extends com.eyecode.javafx.designsystem.FxCard {

    public FxExplorer() {
        getStyleClass().add("explorer-card");
        getStyleClass().remove("fx-card");

        Label label = new Label("Project Explorer");
        label.getStyleClass().add("explorer-placeholder");

        VBox content = new VBox(label);
        content.getStyleClass().add("explorer-content");
        setContent(content);
    }
}
