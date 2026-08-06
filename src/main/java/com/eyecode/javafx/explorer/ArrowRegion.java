package com.eyecode.javafx.explorer;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.css.PseudoClass;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public final class ArrowRegion extends StackPane {

    private static final PseudoClass EXPANDED = PseudoClass.getPseudoClass("expanded");

    private final BooleanProperty expanded = new SimpleBooleanProperty(this, "expanded") {
        @Override
        protected void invalidated() {
            pseudoClassStateChanged(EXPANDED, get());
        }
    };

    public ArrowRegion() {
        getStyleClass().add("arrow-region");
        Region arrow = new Region();
        arrow.getStyleClass().add("arrow");
        getChildren().add(arrow);
    }

    public BooleanProperty expandedProperty() {
        return expanded;
    }
}
