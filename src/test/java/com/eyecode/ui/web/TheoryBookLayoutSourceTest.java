package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TheoryBookLayoutSourceTest {
    @Test
    void theoryRendersAnEditorialArticleWithoutReplacingStructuredBlocks() throws Exception {
        String panel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertTrue(panel.contains("lesson-chapter-title"));
        assertTrue(panel.contains("<h1 className=\"lesson-chapter-title\">{session.title}</h1>"));
        assertFalse(panel.contains("lesson-chapter-meta"));
        assertTrue(panel.contains("lesson-section-marker"));
        assertTrue(panel.contains("++section"));
        assertTrue(panel.contains("lesson-section-divider"));
        assertTrue(panel.contains("lesson-code-frame"));
        assertTrue(panel.contains("navigator.clipboard.writeText"));
        assertTrue(panel.contains("highlightLearningJavaSource"));
        assertTrue(panel.contains("lesson-callout"));
        assertFalse(panel.contains("Aula: {session.title}"));
        assertTrue(styles.contains(".lesson-panel.is-theory .lesson-reading-article"));
        assertTrue(styles.contains("width: 100%"));
        assertTrue(styles.contains("max-width: none"));
        assertTrue(styles.contains("font-family: var(--eyecode-font-ui)"));
        assertTrue(styles.contains("var(--eyecode-font-code)"));
    }
}
