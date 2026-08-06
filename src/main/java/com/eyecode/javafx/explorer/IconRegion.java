package com.eyecode.javafx.explorer;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import com.eyecode.javafx.designsystem.FxSpacing;
import com.eyecode.javafx.designsystem.JavaFxIconManager;
import javafx.scene.layout.StackPane;

public final class IconRegion extends StackPane {

    public IconRegion() {
        getStyleClass().add("icon-region");
    }

    public void update(EyeCodeIcon icon) {
        var imageView = JavaFxIconManager.icon(icon, FxSpacing.ICON_SIZE_DEFAULT);
        imageView.getStyleClass().add("explorer-icon");
        getChildren().setAll(imageView);
    }
}
