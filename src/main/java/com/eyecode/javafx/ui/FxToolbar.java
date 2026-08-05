package com.eyecode.javafx.ui;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.designsystem.FxSpacing;
import com.eyecode.javafx.designsystem.JavaFxIconButton;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;

public final class FxToolbar extends HBox {

    public FxToolbar() {
        this(null);
    }

    public FxToolbar(Runnable onClose) {
        getStyleClass().add("toolbar");
        setPadding(new Insets(0, FxSpacing.TOOLBAR_SIDE_PAD, 0, FxSpacing.TOOLBAR_SIDE_PAD));
        setPrefHeight(FxSpacing.TOOLBAR_HEIGHT);
        setMinHeight(FxSpacing.TOOLBAR_HEIGHT);

        HBox left = new HBox();
        left.getStyleClass().add("toolbar-left");
        Label logo = logoLabel();
        HBox.setMargin(logo, new Insets(0, FxSpacing.XXL, 0, FxSpacing.XXL));
        left.getChildren().addAll(
                JavaFxIconButton.create(EyeCodeIcon.HAMBURGER, "Menu"),
                logo,
                JavaFxIconButton.create(EyeCodeIcon.PROJECT, "Project"),
                projectLabel()
        );

        HBox actions = new HBox();
        actions.getStyleClass().add("toolbar-actions");
        actions.getChildren().addAll(
                JavaFxIconButton.create(EyeCodeIcon.SEARCH, "Search"),
                JavaFxIconButton.create(EyeCodeIcon.GIT, "Git"),
                separator(),
                runConfiguration(),
                JavaFxIconButton.create(EyeCodeIcon.RUN, "Run"),
                JavaFxIconButton.create(EyeCodeIcon.STOP, "Stop"),
                JavaFxIconButton.create(EyeCodeIcon.DEBUG, "Debug"),
                separator(),
                JavaFxIconButton.create(EyeCodeIcon.SETTINGS, "Settings")
        );

        Button closeBtn = windowButton(EyeCodeIcon.CLOSE, "win-close", "Close", onClose);

        HBox windowControls = new HBox();
        windowControls.getStyleClass().add("toolbar-window");
        windowControls.getChildren().addAll(
                windowButton(EyeCodeIcon.MINIMIZE, "win-min", "Minimize", null),
                windowButton(EyeCodeIcon.MAXIMIZE, "win-max", "Maximize", null),
                closeBtn
        );

        HBox right = new HBox();
        right.getStyleClass().add("toolbar-right");
        right.getChildren().addAll(actions, windowControls);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        getChildren().addAll(left, spacer, right);
    }

    private Button windowButton(EyeCodeIcon icon, String id, String tooltip, Runnable onClose) {
        Button b = JavaFxIconButton.windowButton(icon, id, tooltip);
        if (onClose != null) {
            b.setOnAction(e -> onClose.run());
        }
        return b;
    }

    private Button runConfiguration() {
        Button b = JavaFxIconButton.create(EyeCodeIcon.PLAY, "Run Configuration");
        b.setText("Default");
        b.setContentDisplay(javafx.scene.control.ContentDisplay.LEFT);
        b.getStyleClass().add("toolbar-run-config");
        return b;
    }

    private Label logoLabel() {
        Label l = new Label("EyeCode");
        l.getStyleClass().add("toolbar-logo");
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
