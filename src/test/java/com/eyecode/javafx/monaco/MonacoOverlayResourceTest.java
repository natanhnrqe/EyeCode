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
            assertTrue(page.contains("eyecode-dark"));
            assertTrue(page.contains("eyecode-overlays.css"));
            assertTrue(page.contains("highlight.min.js"));
            assertTrue(page.contains("defineTheme"));
            assertTrue(page.contains("editorBracketMatch.background"));
            assertTrue(page.contains("bracketPairColorization"));
            assertTrue(page.contains("semanticIconFor"));
            assertTrue(page.contains("commonMethods"));
            assertTrue(page.contains("highlightLearningCode"));
            assertTrue(page.contains("eyecode-learning-common-methods"));
            assertTrue(page.contains("eyecode-learning-size-quick"));
            assertTrue(page.contains("requestEyeCodeCompletion"));
            assertTrue(page.contains("requestCompletion"));
            assertTrue(page.contains("showEyeCodeCompletion"));
            assertTrue(page.contains("token.onCancellationRequested"));
            assertTrue(page.contains("latestCompletionRequestByModel"));
            assertTrue(page.contains("COMPLETION_REQUEST_TIMEOUT_MS"));
            assertTrue(page.contains("bridge request failed"));
            assertTrue(page.contains("response mapping failed"));
            assertTrue(page.contains("pending.settled"));
            assertTrue(page.contains("EYECODE_JS_RECEIVE"));
            assertTrue(page.contains("content: model.getValue()"));
            assertTrue(page.contains("wordBasedSuggestions: false"));
            assertTrue(page.contains("quickSuggestions: false"));
            assertTrue(page.contains("suggestOnTriggerCharacters: false"));
            assertTrue(page.contains("<link rel=\"stylesheet\" href=\"../eyecode-overlays.css\">"));
            assertTrue(!page.contains("<style>"));
            assertTrue(page.contains("eyecode-learning-body"));
            assertTrue(page.contains("NAVIGATE_LEARNING"));
            assertTrue(page.contains("editorSemanticTargetHovered"));
            assertTrue(page.contains("learningOverlayHovered"));
            assertTrue(page.contains("entryGracePending"));
            assertTrue(page.contains("lastHoverRangeKey"));
            assertTrue(page.contains("getWordAtPosition"));
            assertTrue(page.contains("scheduleHandoffHide"));
            assertTrue(page.contains("if (!command.hard)"));
            assertTrue(page.contains("meta charset=\"UTF-8\""));
        }
    }

    @Test
    void sharedThemeResourceContainsEyeCodePaletteTokens() throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/monaco/eyecode-theme.css")) {
            String theme = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(theme.contains("--eyecode-editor-bg: #191a1c"));
            assertTrue(theme.contains("--eyecode-accent: #3574f0"));
            assertTrue(theme.contains("--eyecode-syntax-keyword: #cf8e6d"));
            assertTrue(theme.contains("--eyecode-syntax-type: #4ec9b0"));
            assertTrue(theme.contains("--eyecode-font-code: \"JetBrains Mono\""));
        }

        try (InputStream stream = getClass().getResourceAsStream("/monaco/eyecode-overlays.css")) {
            String overlays = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
            assertTrue(overlays.contains(".eyecode-learning-body pre code"));
            assertTrue(overlays.contains("overflow-y: auto"));
            assertTrue(overlays.contains("overflow-x: auto"));
            assertTrue(overlays.contains(".hljs-keyword"));
            assertTrue(overlays.contains(".eyecode-learning-common-methods"));
            assertTrue(overlays.contains(".eyecode-learning-card a:visited"));
            assertTrue(overlays.contains(".eyecode-learning-card a:focus-visible"));
            assertTrue(overlays.contains("font: 11px/1.35 var(--eyecode-font-code)"));
            assertTrue(overlays.contains("background: transparent"));
            assertTrue(overlays.contains("pointer-events: none"));
            assertTrue(overlays.contains("pointer-events: auto"));
        }
    }
}
