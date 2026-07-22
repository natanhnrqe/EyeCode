package com.eyecode.learning.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningContentEngineTest {

    private final LearningContentEngine engine = new LearningContentEngine();

    @Test
    void convertsMarkdownToHtml5() {
        String html = engine.convert("# Title\n\nParagraph with **bold**.");

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<html>"));
        assertTrue(html.contains("<body>"));
        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("<p>Paragraph with <strong>bold</strong>.</p>"));
    }

    @Test
    void blankMarkdownStillProducesHtml5() {
        String html = engine.convert("   ");

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<body>"));
        assertTrue(html.contains("</html>"));
    }

    @Test
    void loadsMarkdownResourceAndConvertsToHtml5() {
        String html = engine.loadHtml("/learning/sprint31-sample.md");

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<h1>Learning Engine</h1>"));
        assertTrue(html.contains("<li>First item</li>"));
    }
}
