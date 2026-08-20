package com.eyecode.learning.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningContentRepositoryTest {

    private final LearningContentRepository repository = new LearningContentRepository();

    @Test
    void loadsBundledMarkdownByLogicalIdentifier() {
        String markdown = repository.load("java/basics/variables");

        assertTrue(markdown.startsWith("# Java Variables"));
        assertTrue(markdown.contains("String playerName = \"Ada\""));
    }

    @Test
    void producesDeterministicClasspathResourcePath() {
        assertEquals("/learning/content/java/basics/variables.md",
                repository.resourcePath("/java/basics/variables/"));
    }

    @Test
    void rejectsMissingOrUnsafeIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> repository.load(""));
        assertThrows(IllegalArgumentException.class, () -> repository.load("../variables"));
        assertThrows(IllegalArgumentException.class, () -> repository.load("java/basics/missing"));
    }
}
