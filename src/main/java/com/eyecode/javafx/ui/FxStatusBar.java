package com.eyecode.javafx.ui;

import com.eyecode.javafx.designsystem.FxSpacing;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class FxStatusBar extends HBox {

    public FxStatusBar() {
        getStyleClass().add("status-bar");
        setPadding(new Insets(0, FxSpacing.STATUSBAR_SIDE, 0, FxSpacing.STATUSBAR_SIDE));

        HBox left = new HBox();
        left.getStyleClass().add("status-section");
        left.getChildren().addAll(
                item("status-logo", "EyeCode"),
                separator(),
                item("status-project", "No project"),
                separator(),
                statusItem("status-status", "Ready", "status-dot")
        );

        Region path = new Region();
        HBox.setHgrow(path, javafx.scene.layout.Priority.ALWAYS);
        StackPane pathContainer = new StackPane();
        pathContainer.getStyleClass().add("status-path-container");
        Label pathLabel = new Label("");
        pathLabel.getStyleClass().add("status-path");
        pathContainer.getChildren().add(pathLabel);

        HBox right = new HBox();
        right.getStyleClass().add("status-section");
        right.getChildren().addAll(
                item("status-language", "Plain Text"),
                separator(),
                item("status-encoding", "UTF-8"),
                separator(),
                item("status-line-separator", "LF"),
                separator(),
                item("status-position", "Ln 1, Col 1")
        );

        getChildren().addAll(left, pathContainer, right);
    }

    private Label item(String id, String text) {
        Label l = new Label(text);
        l.setId(id);
        l.getStyleClass().add("status-item");
        return l;
    }

    private Label statusItem(String id, String text, String extraClass) {
        Label l = item(id, text);
        l.getStyleClass().add(extraClass);
        return l;
    }

    private Region separator() {
        Region s = new Region();
        s.getStyleClass().add("status-separator");
        return s;
    }
}
