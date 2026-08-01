package com.eyecode.javafx.ui;

import javafx.scene.control.Button;
import javafx.scene.layout.VBox;

public final class FxActivityBar extends VBox {

    public FxActivityBar() {
        getStyleClass().add("activity-bar");
        setPrefWidth(64);
        setMinWidth(64);
        setMaxWidth(64);

        getChildren().addAll(
                activityButton("project", "\u25C9", "Project"),
                activityButton("terminal", "\u25A0", "Terminal"),
                activityButton("run", "\u25B6", "Run")
        );
    }

    private Button activityButton(String id, String glyph, String tooltip) {
        Button b = new Button(glyph);
        b.setId(id);
        b.getStyleClass().add("activity-btn");
        b.setTooltip(new javafx.scene.control.Tooltip(tooltip));
        return b;
    }
}
