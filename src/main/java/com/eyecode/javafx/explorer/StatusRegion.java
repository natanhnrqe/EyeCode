package com.eyecode.javafx.explorer;

import javafx.scene.control.Label;

public final class StatusRegion extends Label {

    public StatusRegion() {
        getStyleClass().add("status-region");
        setVisible(false);
        setManaged(false);
    }

    public void update(String status) {
        boolean present = status != null && !status.isEmpty();
        setVisible(present);
        setManaged(present);
        setText(status);
    }
}
