package com.eyecode.javafx.designsystem;

import javafx.scene.control.Label;

public final class FxStatusItem extends Label {

    public FxStatusItem(String text) {
        super(text);
        getStyleClass().add("status-item");
    }
}