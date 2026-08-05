package com.eyecode.javafx.ui;

import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;

public final class FxConsole extends com.eyecode.javafx.designsystem.FxCard {

    private final TabPane tabPane;

    public FxConsole() {
        getStyleClass().add("terminal-card");
        getStyleClass().remove("fx-card");

        tabPane = new TabPane();
        tabPane.getStyleClass().add("console-tabs");

        Tab terminalTab = new Tab("Terminal", buildPlaceholder("Terminal placeholder"));
        terminalTab.setClosable(false);
        Tab runTab = new Tab("Run", buildPlaceholder("Run output placeholder"));
        runTab.setClosable(false);

        tabPane.getTabs().addAll(terminalTab, runTab);

        setContent(tabPane);
    }

    private Label buildPlaceholder(String text) {
        Label l = new Label(text);
        l.getStyleClass().add("console-placeholder");
        return l;
    }
}
