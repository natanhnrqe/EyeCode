package com.eyecode.learning.catalog;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSyntaxLearningResolverTest {

    private final JavaSyntaxLearningResolver resolver = new JavaSyntaxLearningResolver();

    @Test
    void resolvesModernConstructsOnlyInTheirContext() {
        assertEquals("java/syntax/types/var",
                resolver.resolve("var nome = \"EyeCode\";", 1).orElseThrow().getPage().getId());
        assertEquals("java/types/record",
                resolver.resolve("record Pessoa(String nome) {}", 1).orElseThrow().getPage().getId());
        assertEquals("java/syntax/types/sealed",
                resolver.resolve("sealed interface Forma permits Circulo {}", 1).orElseThrow().getPage().getId());
        assertEquals("java/syntax/types/permits",
                resolver.resolve("sealed interface Forma permits Circulo {}", 25).orElseThrow().getPage().getId());
        assertEquals("java/syntax/control-flow/yield",
                resolver.resolve("int x = switch (n) { default -> { yield 1; } };",
                        36).orElseThrow().getPage().getId());
    }

    @Test
    void rejectsRestrictedWordsUsedOutsideTheirConstruct() {
        assertTrue(resolver.resolve("int var = 10;", 4).isEmpty());
        assertTrue(resolver.resolve("void record() {}", 5).isEmpty());
        assertTrue(resolver.resolve("int record = 1;", 4).isEmpty());
        assertTrue(resolver.resolve("non-sealed class Child {}", 3).isEmpty());
        assertTrue(resolver.resolve("void yieldValue() {}", 6).isEmpty());
    }

    @Test
    void preservesTrueKeywordLearning() {
        assertEquals("java/syntax/types/instanceof",
                resolver.resolve("value instanceof String", 8).orElseThrow().getPage().getId());
        assertEquals("java/syntax/control-flow/switch",
                resolver.resolve("switch (value) {}", 1).orElseThrow().getPage().getId());
        assertEquals("java/syntax/control-flow/case",
                resolver.resolve("switch (value) { case 1: break; }", 18).orElseThrow().getPage().getId());
        assertEquals("java/syntax/control-flow/default",
                resolver.resolve("switch (value) { default: break; }", 18).orElseThrow().getPage().getId());
    }
}
