package com.eyecode.javafx.explorer;

import javafx.scene.control.Label;

public final class BadgeRegion extends Label {

    public BadgeRegion() {
        getStyleClass().add("badge-region");
        setVisible(false);
        setManaged(false);
    }

    public void update(String badge) {
        boolean present = badge != null && !badge.isEmpty();
        setVisible(present);
        setManaged(present);
        setText(badge);
    }
}
