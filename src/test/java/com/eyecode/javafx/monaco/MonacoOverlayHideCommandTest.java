package com.eyecode.javafx.monaco;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MonacoOverlayHideCommandTest {
    @Test
    void hideCommandDistinguishesSoftAndHardLifecycleRequests() {
        String soft = JavaFxMonacoEditorSurface.commandJsonForTest(
                new MonacoCommand.HideOverlay("learning", 3, false));
        String hard = JavaFxMonacoEditorSurface.commandJsonForTest(
                new MonacoCommand.HideOverlay("learning", 4, true));

        assertTrue(soft.contains("\"hard\":false"));
        assertTrue(hard.contains("\"hard\":true"));
    }
}
