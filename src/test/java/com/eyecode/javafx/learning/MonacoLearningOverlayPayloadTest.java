package com.eyecode.javafx.learning;

import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.learning.content.LearningMember;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

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
        assertTrue(json.contains("\"sizeClass\":\"full\""));
        assertTrue(json.contains("\"iconKind\":\"LEARNING\""));
        assertTrue(json.contains("\"iconUrl\":\"data:image/svg+xml;base64,"));
        assertEquals("full", payload.sizeClass());
        assertEquals("LEARNING", payload.iconKind());
        assertTrue(payload.iconUrl().startsWith("data:image/svg+xml;base64,"));
        assertTrue(payload.breadcrumb().isEmpty());
    }

    @Test
    void serializesOnlyRealCommonMethodTargetsAndKeepsNestedBreadcrumb() {
        LearningMetadata root = new LearningMetadata(
                "java/jdk/string", "String", "string", "beginner", 4,
                "JAVA API", null, List.of(), null);
        LearningMetadata member = new LearningMetadata(
                "java/jdk/string/substring", "String.substring()", "string-substring",
                "beginner", 2, "JAVA API", null, List.of(), null);
        MonacoLearningOverlayPayload payload = MonacoLearningOverlayPayload.from(
                member, List.of(root), "<pre><code class=\"language-java\">String s;</code></pre>",
                List.of(new LearningMember("substring()", member.id())), List.of(), null, false);

        assertEquals(List.of(new MonacoLearningOverlayPayload.Item(root.id(), root.title()),
                new MonacoLearningOverlayPayload.Item(member.id(), member.title())), payload.breadcrumb());
        assertEquals(List.of(new MonacoLearningOverlayPayload.Item(member.id(), "substring()")),
                payload.commonMethods());
        assertTrue(payload.json().contains("commonMethods"));
        assertTrue(payload.renderedBodyHtml().contains("<pre><code class=\"language-java\">"));
    }
}
