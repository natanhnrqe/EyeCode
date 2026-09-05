package com.eyecode.learning.content;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.eyecode.learning.content.LearningResourceLoader;

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
        String html = engine.convert("""
                # Variáveis

                Um parágrafo com `int`.

                1. Primeiro passo
                2. Segundo passo

                > Uma citação.

                | Tipo | Exemplo |
                | --- | --- |
                | int | 20 |

                ```java
                int age = 20;
                ```
                """);

        assertTrue(html.contains("<h1>Variáveis</h1>"));
        assertTrue(html.contains("<p>Um parágrafo com <code>int</code>.</p>"));
        assertTrue(html.contains("<ol>"));
        assertTrue(html.contains("<pre><code class=\"language-java\">"));
        assertTrue(html.contains("<blockquote>"));
        assertTrue(html.contains("<table>"));
    }

    @Test
    void loadsBundledVariablesLessonAsPortugueseContent() {
        LearningDocument document = engine.loadDocument("java/basics/variables");

        assertEquals("Variáveis em Java", document.metadata().title());
        assertFalse(document.markdownBody().isBlank());
        assertTrue(document.renderedHtml().contains("<pre><code class=\"language-java\">"));
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
    void reusesRenderedDocumentForRepeatedIdentifier() {
        LearningDocument first = engine.loadDocument("java/types/object");
        LearningDocument second = engine.loadDocument("java/types/object");

        assertSame(first, second);
    }

    @Test
    void cachesDocumentsByIdentifierWithoutReusingDifferentLessonContent() {
        LearningDocument object = engine.loadDocument("java/types/object");
        LearningDocument string = engine.loadDocument("java/jdk/string");

        assertNotSame(object, string);
        assertEquals("java/types/object", object.identifier());
        assertEquals("java/jdk/string", string.identifier());
    }

    @Test
    void loadsStringMemberLessonsByStableIdentifiers() {
        LearningDocument document = engine.loadDocument("java/jdk/string/substring");

        assertEquals("String.substring()", document.metadata().title());
        assertEquals("quick", document.metadata().depth().name().toLowerCase());
        assertTrue(document.renderedHtml().contains("substring(begin, end)"));
    }

    @Test
    void rendersSupportedInfoAndWarningCallouts() {
        String html = engine.convert(
                "> [!INFO]\n> A value is available.\n\n> [!WARNING]\n> Be careful.");

        assertTrue(html.contains("learning-callout learning-callout-info"));
        assertTrue(html.contains("learning-callout learning-callout-warning"));
    }

    @Test
    void learningStylesAllowBodyScrollingAndLocalCodeOverflow() {
        String css = new LearningResourceLoader().load("/learning/css/learning.css");

        assertTrue(css.contains("body.learning-markdown"));
        assertTrue(css.contains("overflow-y: auto"));
        assertTrue(css.contains("overflow-x: hidden"));
        assertTrue(css.contains("pre {"));
        assertTrue(css.contains("overflow-x: auto"));
    }
}
