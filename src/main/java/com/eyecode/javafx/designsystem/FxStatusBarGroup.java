package com.eyecode.javafx.designsystem;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public final class FxStatusBarGroup extends HBox {

    public FxStatusBarGroup() {
        getStyleClass().add("status-group");
    }

    public void addItem(FxStatusItem item) {
        getChildren().add(item);
    }
}