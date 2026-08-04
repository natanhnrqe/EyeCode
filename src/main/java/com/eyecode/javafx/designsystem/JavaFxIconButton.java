package com.eyecode.javafx.designsystem;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;

public final class JavaFxIconButton {

    private JavaFxIconButton() {}

    public static Button create(EyeCodeIcon icon, String tooltip) {
        return create(icon, tooltip, FxSpacing.ICON_SIZE_DEFAULT);
    }

    public static Button create(EyeCodeIcon icon, String tooltip, double iconSize) {
        Button b = new Button();
        b.getStyleClass().addAll("toolbar-btn", "toolbar-btn-icon");
        b.setGraphic(JavaFxIconManager.icon(icon, iconSize));
        b.setContentDisplay(javafx.scene.control.ContentDisplay.GRAPHIC_ONLY);
        if (tooltip != null && !tooltip.isBlank()) {
            b.setTooltip(new Tooltip(tooltip));
        }
        return b;
    }

    public static Button windowButton(EyeCodeIcon icon, String id, String tooltip) {
        Button b = create(icon, tooltip, FxSpacing.ICON_SIZE_DEFAULT);
        b.getStyleClass().add("win-btn");
        b.setId(id);
        return b;
    }
}
