package com.eyecode.learning.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentationTargetTest {

    @Test
    void acceptsHttpAndHttpsTargets() {
        assertEquals("String", new DocumentationTarget("String", "https://docs.oracle.com/string").label());
        assertEquals("http://example.test", new DocumentationTarget("Example", "http://example.test").url());
    }

    @Test
    void rejectsUnsupportedSchemes() {
        assertThrows(IllegalArgumentException.class,
                () -> new DocumentationTarget("Unsafe", "javascript:alert(1)"));
    }
}
