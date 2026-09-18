package com.eyecode.javafx.monaco;

import com.eyecode.ui.web.monaco.MonacoCommand;
import com.eyecode.ui.web.monaco.MonacoOverlayType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MonacoOverlayUnicodeSerializationTest {
    @Test
    void commandJsonEscapesUnicodeForJcefScriptBoundary() {
        String payload = "{\"title\":\"📘 O que é isso?\",\"body\":\"💡 ação métodos → resultado\"}";
        String json = JavaFxMonacoEditorSurface.commandJsonForTest(
                new MonacoCommand.ShowOverlay("learning", MonacoOverlayType.LEARNING,
                        1, 1, payload, 9));

        assertTrue(json.contains("\\ud83d\\udcd8"));
        assertTrue(json.contains("\\ud83d\\udca1"));
        assertTrue(json.contains("\\u00e7"));
        assertTrue(json.contains("\\u00e3"));
        assertTrue(json.contains("\\u00e9"));
        assertTrue(json.contains("\\u2192"));
    }
}
