package com.eyecode.javafx.designsystem;

import javafx.scene.control.Button;

public final class JavaFxButton {

    private JavaFxButton() {
    }

    public static Button create(String text) {
        Button button = new Button(text);
        button.getStyleClass().add("eyecode-button");
        return button;
    }

    public static Button primary(String text) {
        Button button = create(text);
        button.getStyleClass().add("eyecode-button-primary");
        return button;
    }
}
