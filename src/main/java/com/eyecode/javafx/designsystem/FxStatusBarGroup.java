package com.eyecode.javafx.designsystem;

import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public final class FxStatusBarGroup extends HBox {

    public FxStatusBarGroup() {
        // placeholder container for FxStatusItem placeholders
    }

    public void addItem(FxStatusItem item) {
        if (!getChildren().isEmpty()) {
            getChildren().add(separator());
        }
        getChildren().add(item);
    }

    private Region separator() {
        Region s = new Region();
        s.getStyleClass().add("status-separator");
        return s;
    }
}