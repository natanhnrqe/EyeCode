package com.eyecode.javafx.ui.toolwindow.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningHtmlPayloadTest {

    @Test
    void serializesUnicodeContentAsAnAsciiJsonTransport() {
        String script = LearningHtmlPayload.updateScript(
                "📘 O que é isso? → próxima etapa ✓ ação, criação, métodos String \"EyeCode\" \\\\"
        );

        assertTrue(script.startsWith("window.eyeCodeLearningUpdate(JSON.parse("));
        assertTrue(script.contains("\\\\ud83d\\\\udcd8"));
        assertTrue(script.contains("\\\\u00e7"));
        assertTrue(script.contains("\\\\u00e3"));
        assertTrue(script.contains("\\\\u2192"));
        assertTrue(script.contains("\\\\\\\"EyeCode\\\\\\\""));
        assertFalse(script.chars().anyMatch(character -> character > 0x7e));
    }
}
