package com.eyecode.javafx.ui.toolwindow.content;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.util.Map;

public final class GenericToolWindowContent extends VBox {

    private static final Map<String, String> TITLES = Map.of(
            "run",           "Run",
            "terminal",      "Terminal",
            "output",        "Output",
            "problems",      "Problems",
            "git",           "Git",
            "professor-ia",  "Professor IA",
            "extensions",    "Extensions",
            "profile",       "Profile"
    );

    private GenericToolWindowContent() {
        getStyleClass().add("toolwindow-content");
    }

    public static GenericToolWindowContent forId(String id) {
        GenericToolWindowContent content = new GenericToolWindowContent();
        Label label = new Label(TITLES.getOrDefault(id, id));
        label.getStyleClass().add("toolwindow-placeholder");
        content.getChildren().add(label);
        return content;
    }
}
