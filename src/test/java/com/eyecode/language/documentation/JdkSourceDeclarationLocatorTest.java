package com.eyecode.language.documentation;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JdkSourceDeclarationLocatorTest {

    private final JdkSourceDeclarationLocator locator = new JdkSourceDeclarationLocator();

    @Test
    void findsTypeDeclarationInsteadOfEndOfFile() {
        String source = "/* String */\npublic final class String {\n}\n";

        assertEquals(source.indexOf("class String"), locator.find(source, "String"));
    }

    @Test
    void ignoresCommentsAndFallsBackSafely() {
        String source = "// class String\nvoid method() {}";

        assertEquals(0, locator.find(source, "String"));
    }
}
