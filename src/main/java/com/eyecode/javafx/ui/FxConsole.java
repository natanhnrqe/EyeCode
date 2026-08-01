package com.eyecode.javafx.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public final class FxConsole extends TabPane {

    public FxConsole() {
        getStyleClass().add("console");

        Tab terminalTab = new Tab("Terminal", buildPlaceholder("Terminal placeholder"));
        terminalTab.setClosable(false);
        Tab runTab = new Tab("Run", buildPlaceholder("Run output placeholder"));
        runTab.setClosable(false);

        getTabs().addAll(terminalTab, runTab);
        setPrefHeight(300);
    }

    private Label buildPlaceholder(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("console-placeholder");
        return l;
    }
}
