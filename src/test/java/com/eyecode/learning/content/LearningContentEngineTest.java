package com.eyecode.learning.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;

class LearningContentEngineTest {

    private final LearningContentEngine engine = new LearningContentEngine();

    @Test
    void convertsMarkdownToHtml5() {
        String html = engine.convert("# Title\n\nParagraph with **bold**.");

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<html>"));
        assertTrue(html.contains("<body class=\"learning-markdown\">"));
        assertTrue(html.contains("<h1>Title</h1>"));
        assertTrue(html.contains("<p>Paragraph with <strong>bold</strong>.</p>"));
    }

    @Test
    void blankMarkdownStillProducesHtml5() {
        String html = engine.convert("   ");

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<body class=\"learning-markdown\">"));
        assertTrue(html.contains("</html>"));
    }

    @Test
    void loadsMarkdownResourceAndConvertsToHtml5() {
        String html = engine.loadHtml("/learning/sprint31-sample.md");

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<h1>Learning Engine</h1>"));
        assertTrue(html.contains("<li>First item</li>"));
    }

    @Test
    void rendersBundledLessonMarkdownFeatures() {
        String html = engine.loadHtml("/learning/content/java/basics/variables.md");

        assertTrue(html.contains("Variables give a name to data"));
        assertTrue(html.contains("<ul>"));
        assertTrue(html.contains("<ol>"));
        assertTrue(html.contains("<code>int</code>"));
        assertTrue(html.contains("<pre><code class=\"language-java\">"));
        assertTrue(html.contains("<blockquote>"));
        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("href=\"https://docs.oracle.com/javase/specs/\""));
    }

    @Test
    void rendersLogicalLessonIdentifierDeterministically() {
        String first = engine.loadHtmlByIdentifier("java/basics/variables");
        String second = engine.loadHtmlByIdentifier("java/basics/variables");

        assertTrue(first.contains("<main class=\"learning-content\">"));
        assertTrue(first.contains("href=\"data:text/css;base64,"));
        assertTrue(first.equals(second));
    }

    @Test
    void loadsDocumentWithMetadataAndRenderedBody() {
        LearningDocument document = engine.loadDocument("java/types/object");

        assertTrue(document.renderedHtml().contains("<main class=\"learning-content\">"));
        assertTrue(document.renderedHtml().contains("objeto"));
        assertEquals(document.metadata().id(), document.identifier());
    }

    @Test
    void rendersSupportedInfoAndWarningCallouts() {
        String html = engine.convert(
                "> [!INFO]\n> A value is available.\n\n> [!WARNING]\n> Be careful.");

        assertTrue(html.contains("learning-callout learning-callout-info"));
        assertTrue(html.contains("learning-callout learning-callout-warning"));
    }
}
