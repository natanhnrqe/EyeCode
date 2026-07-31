package com.eyecode.javafx;

import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.Priority;

public final class FxToolbar extends HBox {

    public FxToolbar() {
        this(null);
    }

    public FxToolbar(Runnable onClose) {
        getStyleClass().add("toolbar");

        HBox left = new HBox();
        left.getStyleClass().add("toolbar-left");
        left.getChildren().addAll(
                iconButton("hamburger", "\u2630"),
                logoLabel(),
                projectLabel()
        );

        HBox actions = new HBox();
        actions.getStyleClass().add("toolbar-actions");
        actions.getChildren().addAll(
                iconButton("run", "\u25B6"),
                iconButton("stop", "\u25A0"),
                iconButton("debug", "\u25C7"),
                separator(),
                iconButton("open", "\u25C9"),
                iconButton("save", "\u25C0"),
                separator(),
                iconButton("search", "\u25C5"),
                iconButton("settings", "\u2699")
        );

        Button closeBtn = iconButton("win-close", "\u2715");
        if (onClose != null) {
            closeBtn.setOnAction(e -> onClose.run());
        }

        HBox windowControls = new HBox();
        windowControls.getStyleClass().add("toolbar-window");
        windowControls.getChildren().addAll(
                iconButton("win-min", "\u2014"),
                iconButton("win-max", "\u25A1"),
                closeBtn
        );

        HBox right = new HBox();
        right.getStyleClass().add("toolbar-right");
        right.getChildren().addAll(actions, windowControls);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(left, spacer, right);
    }

    private Button iconButton(String id, String glyph) {
        Button b = new Button(glyph);
        b.setId(id);
        b.getStyleClass().add("toolbar-btn");
        return b;
    }

    private Label logoLabel() {
        Label l = new Label("EyeCode");
        l.getStyleClass().add("toolbar-logo");
        HBox.setMargin(l, new Insets(0, 12, 0, 12));
        return l;
    }

    private Label projectLabel() {
        Label l = new Label("No project");
        l.getStyleClass().add("toolbar-project");
        return l;
    }

    private Region separator() {
        Region s = new Region();
        s.getStyleClass().add("toolbar-separator");
        return s;
    }
}
