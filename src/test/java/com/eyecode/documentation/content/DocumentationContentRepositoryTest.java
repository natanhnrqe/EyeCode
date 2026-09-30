package com.eyecode.documentation.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentationContentRepositoryTest {

    private final DocumentationContentRepository repository = new DocumentationContentRepository();

    @Test
    void resourcePathNormalizesSeparators() {
        assertEquals("/documentation/content/java/jdk/fundamentos/variables.md",
                repository.resourcePath("java/jdk/fundamentos/variables"));
        assertEquals("/documentation/content/java/jdk/fundamentos/variables.md",
                repository.resourcePath("/java/jdk/fundamentos/variables/"));
        assertEquals("/documentation/content/java/jdk/fundamentos/variables.md",
                repository.resourcePath("java\\jdk\\fundamentos\\variables"));
    }

    @Test
    void resourcePathRejectsBlankAndTraversal() {
        assertThrows(IllegalArgumentException.class, () -> repository.resourcePath(""));
        assertThrows(IllegalArgumentException.class, () -> repository.resourcePath("   "));
        assertThrows(IllegalArgumentException.class, () -> repository.resourcePath("../secrets"));
        assertThrows(IllegalArgumentException.class, () -> repository.resourcePath("java/../../etc"));
    }

    @Test
    void loadsRealPageFromBundle() {
        String markdown = repository.load("java/jdk/fundamentos/variables");

        assertTrue(markdown.startsWith("---"));
        assertTrue(markdown.contains("title: Variáveis em Java"));
        assertTrue(markdown.contains("## Profundidade"));
    }

    @Test
    void loadRejectsUnknownPage() {
        assertThrows(IllegalArgumentException.class, () -> repository.load("java/jdk/nao-existe"));
    }

    @Test
    void catalogListsAllDemoPagesIgnoringComments() {
        List<String> identifiers = repository.catalogIdentifiers();

        assertEquals(67, identifiers.size());
        assertEquals("java/jdk/fundamentos/variables", identifiers.getFirst());
        assertTrue(identifiers.contains("java/spring/boot-basics"));
        assertTrue(identifiers.contains("java/junit/first-test"));
        assertTrue(identifiers.stream().noneMatch(id -> id.startsWith("#")));
    }

    @Test
    void frontMatterOfRealPageCarriesMetadata() {
        DocumentationFrontMatter metadata = repository.loadFrontMatter("java/jdk/fundamentos/classes");

        assertEquals("java/jdk/fundamentos/classes", metadata.id());
        assertEquals("Classes e Objetos", metadata.title());
        assertEquals("beginner", metadata.level());
        assertTrue(metadata.duration() > 0);
        assertEquals(List.of("java/jdk/fundamentos/variables", "java/jdk/java.lang/string"),
                metadata.related());
    }
}
