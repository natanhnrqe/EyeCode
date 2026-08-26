package com.eyecode.javafx.learning;

import com.eyecode.learning.content.LearningMetadata;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MonacoLearningOverlayPayloadTest {
    @Test
    void serializesStructuredContentWithoutLosingUnicodeOrMarkup() {
        LearningMetadata metadata = new LearningMetadata(
                "java/types/example", "Exemplo çãé 📘", "example", "beginner", 2,
                "Java", null, List.of(), null);
        MonacoLearningOverlayPayload payload = MonacoLearningOverlayPayload.from(
                metadata, List.of(), "<p>Use ✓ → \"quotes\" and \\\\.</p>",
                List.of(), null, false);

        String json = payload.json();
        assertTrue(json.contains("Exemplo çãé 📘"));
        assertTrue(json.contains("\\\"quotes\\\""));
        assertTrue(json.contains("renderedBodyHtml"));
    }
}
