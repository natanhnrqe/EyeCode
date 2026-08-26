package com.eyecode.javafx.monaco;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MonacoOverlayResourceTest {

    @Test
    void editorPageContainsPersistentContextLayerAndGenerationGuard() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/monaco/editor/index.html")) {
            String page = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(page.contains("eyecode-context-layer"));
            assertTrue(page.contains("showContextOverlay"));
            assertTrue(page.contains("overlayGenerations"));
            assertTrue(page.contains("pointerenter"));
            assertTrue(page.contains("pointerleave"));
            assertTrue(page.contains("getScrolledVisiblePosition"));
            assertTrue(page.contains("renderLearningOverlay"));
            assertTrue(page.contains("eyecode-learning-body"));
            assertTrue(page.contains("NAVIGATE_LEARNING"));
            assertTrue(page.contains("pointer-events: none"));
            assertTrue(page.contains("editorSemanticTargetHovered"));
            assertTrue(page.contains("learningOverlayHovered"));
            assertTrue(page.contains("entryGracePending"));
            assertTrue(page.contains("lastHoverRangeKey"));
            assertTrue(page.contains("getWordAtPosition"));
            assertTrue(page.contains("scheduleHandoffHide"));
            assertTrue(page.contains("if (!command.hard)"));
            assertTrue(page.contains("pointer-events: auto"));
            assertTrue(page.contains("meta charset=\"UTF-8\""));
        }
    }
}
