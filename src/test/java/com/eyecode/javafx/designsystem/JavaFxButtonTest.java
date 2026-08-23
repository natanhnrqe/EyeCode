package com.eyecode.javafx.designsystem;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxButtonTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void standardButtonUsesOnlyTheBaseTactileVariant() {
        var button = JavaFxButton.create("Back");

        assertTrue(button.getStyleClass().contains("eyecode-button"));
        assertFalse(button.getStyleClass().contains("eyecode-button-primary"));
    }

    @Test
    void primaryButtonAddsThePrimaryVariant() {
        var button = JavaFxButton.primary("Create Project");

        assertTrue(button.getStyleClass().contains("eyecode-button"));
        assertTrue(button.getStyleClass().contains("eyecode-button-primary"));
    }
}
