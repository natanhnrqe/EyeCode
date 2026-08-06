package com.eyecode.javafx.explorer;

import javafx.scene.control.Label;

public final class TitleRegion extends Label {

    public TitleRegion() {
        getStyleClass().add("title-region");
        setMaxWidth(Double.MAX_VALUE);
    }
}
