package com.eyecode.language;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LanguageFeatureRequestTest {
    private static final LanguageDocument DOCUMENT =
            new LanguageDocument("file:///Main.java", null, "Main.java", LanguageId.JAVA);

    @Test
    void fourArgumentConstructorDefaultsTriggerCharacterToEmpty() {
        var request = new LanguageFeatureRequest(DOCUMENT, 1, "abc", 2);
        assertEquals("", request.triggerCharacter());
    }

    @Test
    void nullTriggerCharacterBecomesEmpty() {
        var request = new LanguageFeatureRequest(DOCUMENT, 1, "abc", 2, null);
        assertEquals("", request.triggerCharacter());
    }

    @Test
    void triggerCharacterIsPreserved() {
        var request = new LanguageFeatureRequest(DOCUMENT, 1, "abc", 2, ",");
        assertEquals(",", request.triggerCharacter());
    }

    @Test
    void caretOffsetIsClampedToSourceBounds() {
        var request = new LanguageFeatureRequest(DOCUMENT, 1, "abc", 99, "(");
        assertEquals(3, request.caretOffset());
    }

    @Test
    void nullDocumentIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new LanguageFeatureRequest(null, 1, "abc", 0, "("));
    }
}
