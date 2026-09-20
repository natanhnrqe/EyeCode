package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PracticeLessonVisualSourceTest {
    @Test
    void practiceUsesItsOwnCompactGuideWithoutReplacingTheoryStructure() throws Exception {
        String panel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertTrue(panel.contains("const practiceLesson = session.kind === 'PRACTICE';"));
        assertTrue(panel.contains("const practiceActive = practiceLesson && session.phase === 'PRACTICE' && session.practice !== undefined;"));
        String learningPanel = Files.readString(Path.of("src/main/web/src/lessons/LearningPanel.tsx"));
        assertTrue(learningPanel.contains("is-practice${phaseClass}"));
        assertTrue(panel.contains("is-presentation"));
        assertTrue(panel.contains("is-practice-active"));
        assertTrue(panel.contains("is-completed"));
        assertTrue(panel.contains("is-practice-completed"));
        assertTrue(panel.contains("lesson-practice-header"));
        assertTrue(panel.contains("lesson-practice-title"));
        assertTrue(panel.contains("lesson-practice-lead\">{session.message}</p>"));
        assertTrue(panel.contains("practiceActive ? <PracticeSupport blocks={session.contentBlocks} />"));
        assertTrue(panel.contains("{theory && <header className=\"lesson-chapter-header\""));
        assertFalse(panel.contains("{practice && <header className=\"lesson-chapter-header\""));
        assertTrue(styles.contains(".lesson-panel.is-practice .lesson-reading-article"));
        assertTrue(styles.contains(".lesson-panel.is-theory .lesson-reading-article"));
        assertTrue(styles.contains(".lesson-panel.is-practice .lesson-heading"));
        assertTrue(styles.contains("font: 700 19px/1.35 var(--eyecode-font-ui);"));
        assertTrue(styles.contains(".lesson-practice-header"));
        assertTrue(block(styles, ".lesson-panel.is-practice .lesson-heading").contains("border: 0;"));
        assertTrue(block(styles, ".lesson-practice-header").contains("padding-bottom: 0;"));
    }

    @Test
    void annotationKeepsItsAnchorAndRendersTheBackendContent() throws Exception {
        String annotation = Files.readString(Path.of("src/main/web/src/lessons/LessonAnnotation.tsx"));

        assertTrue(annotation.contains("service.lessonAnnotationAnchor(lessonUri, annotation.range)"));
        assertTrue(annotation.contains("service.subscribeViewport(update)"));
        assertTrue(annotation.contains("<strong>{annotation.title}</strong><p>{annotation.message}</p>"));
        assertTrue(annotation.contains("if (!annotation || !position) return null;"));
    }

    @Test
    void practiceKeepsTheSharedCodeHighlighterAndCalloutSurface() throws Exception {
        String panel = Files.readString(Path.of("src/main/web/src/lessons/LessonPanel.tsx"));
        String styles = Files.readString(Path.of("src/main/web/src/styles.css"));

        assertTrue(panel.contains("highlightLearningJavaSource(block.code ?? '')"));
        assertTrue(panel.contains("return <aside className=\"lesson-callout\""));
        assertTrue(styles.contains(".lesson-callout {"));
        assertTrue(styles.contains("background: color-mix(in srgb, var(--eyecode-accent-soft) 28%, transparent);"));
    }

    private static String block(String styles, String selector) {
        int start = styles.indexOf(selector);
        return styles.substring(start, styles.indexOf('}', start));
    }
}
