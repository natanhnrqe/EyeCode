package com.eyecode.javafx.ui;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.designsystem.FxSpacing;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.VBox;

public final class FxActivityBar extends VBox {

    public FxActivityBar() {
        getStyleClass().add("activity-bar");
        double w = FxSpacing.ACTIVITY_BAR_WIDTH;
        setPrefWidth(w);
        setMinWidth(w);
        setMaxWidth(w);
        setSpacing(FxSpacing.XS);
        setPadding(new javafx.geometry.Insets(FxSpacing.ACTIVITY_TOP_INSET, 0, 0, 0));

        getChildren().addAll(
                activityButton(EyeCodeIcon.PROJECT,    "project",  "Project",  true),
                activityButton(EyeCodeIcon.SEARCH,     "search",   "Search",   false),
                activityButton(EyeCodeIcon.GIT,        "git",      "Git",      false),
                activityButton(EyeCodeIcon.RUN,        "run",      "Run",      false),
                activityButton(EyeCodeIcon.DEBUG,      "debug",    "Debug",    false),
                activityButton(EyeCodeIcon.SETTINGS,   "settings", "Settings", false)
        );
    }

    private Button activityButton(EyeCodeIcon icon, String id, String tooltip, boolean selected) {
        Button b = new Button();
        b.setId(id);
        b.getStyleClass().addAll("activity-btn");
        if (selected) b.getStyleClass().add("activity-btn-selected");
        b.setGraphic(JavaFxIconManager.icon(icon, FxSpacing.ICON_SIZE_ACTIVITY));
        b.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        b.setTooltip(new Tooltip(tooltip));
        b.setOnAction(e -> select(b));
        return b;
    }

    private void select(Button b) {
        for (var node : getChildren()) {
            if (node instanceof Button btn) {
                btn.getStyleClass().remove("activity-btn-selected");
            }
        }
        if (b != null) {
            b.getStyleClass().add("activity-btn-selected");
        }
    }
}
