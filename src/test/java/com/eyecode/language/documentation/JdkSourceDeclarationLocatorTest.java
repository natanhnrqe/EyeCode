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

    @Test
    void findsMemberDeclarationAndIgnoresInvocationAndComments() {
        String source = """
                public final class String {
                    // contains() is documented here
                    void use() { helper.contains(value); }
                    public boolean contains(CharSequence value) { return true; }
                }
                """;
        JdkSourceTarget target = new JdkSourceTarget(
                "java.lang.String", "java.base", "java.base/java/lang/String.java",
                "String.java", "contains");

        assertEquals(source.indexOf("contains(CharSequence"), locator.find(source, target));
    }

    @Test
    void missingMemberFallsBackToOwnerDeclaration() {
        String source = "public final class String { }";
        JdkSourceTarget target = new JdkSourceTarget(
                "java.lang.String", "java.base", "java.base/java/lang/String.java",
                "String.java", "missing");

        assertEquals(source.indexOf("class String"), locator.find(source, target));
    }

    @Test
    void choosesFirstOverload() {
        String source = "class String { String substring(int start) { return this; } "
                + "String substring(int start, int end) { return this; } }";
        JdkSourceTarget target = new JdkSourceTarget(
                "java.lang.String", "java.base", "java.base/java/lang/String.java",
                "String.java", "substring");

        assertEquals(source.indexOf("substring(int start)"), locator.find(source, target));
    }
}
