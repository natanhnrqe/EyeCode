package com.eyecode.javafx.ui;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.designsystem.FxSpacing;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

public final class FxBottomToolWindowBar extends HBox {

    public FxBottomToolWindowBar() {
        getStyleClass().add("bottom-tool-window-bar");
        setPrefHeight(FxSpacing.BOTTOM_BAR_HEIGHT);
        setMinHeight(FxSpacing.BOTTOM_BAR_HEIGHT);
        setPadding(new Insets(
                FxSpacing.BOTTOM_BAR_BTN_PAD_V,
                FxSpacing.BOTTOM_BAR_SIDE,
                FxSpacing.BOTTOM_BAR_BTN_PAD_V,
                FxSpacing.BOTTOM_BAR_SIDE));
        setSpacing(FxSpacing.XXS);

        getChildren().addAll(
                tabButton(EyeCodeIcon.TERMINAL,                  "Terminal", true),
                textTabButton("Problems",                          false),
                textTabButton("Output",                             false),
                textTabButton("TODO",                               false),
                tabButton(EyeCodeIcon.SERVICES,    "Services",      false)
        );
    }

    private Button tabButton(EyeCodeIcon icon, String tooltip, boolean selected) {
        Button b = new Button();
        b.getStyleClass().addAll("bottom-tw-btn");
        if (selected) b.getStyleClass().add("bottom-tw-btn-selected");
        b.setGraphic(JavaFxIconManager.icon(icon, 14));
        b.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    private Button textTabButton(String text, boolean selected) {
        Button b = new Button(text);
        b.getStyleClass().add("bottom-tw-btn");
        if (selected) b.getStyleClass().add("bottom-tw-btn-selected");
        b.setContentDisplay(javafx.scene.control.ContentDisplay.TEXT_ONLY);
        return b;
    }
}
