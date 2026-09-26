package com.eyecode.language.hover;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HoverHtmlRendererTest {

    @Test
    void markdownRendersHtmlFragment() {
        String html = HoverHtmlRenderer.render(List.of(new HoverContent("markdown", "**bold** text")));

        assertTrue(html.startsWith("<div class=\"hover-doc\">"));
        assertTrue(html.contains("<p><strong>bold</strong> text</p>"));
    }

    @Test
    void javaRendersHighlightedCodeBlock() {
        String html = HoverHtmlRenderer.render(List.of(new HoverContent("java", "int x = 1;")));

        assertTrue(html.contains("<pre><code class=\"language-java\">int x = 1;</code></pre>"));
    }

    @Test
    void plaintextEscapesHtml() {
        String html = HoverHtmlRenderer.render(List.of(new HoverContent("plaintext", "<tag> & more")));

        assertTrue(html.contains("<p>&lt;tag&gt; &amp; more</p>"));
    }

    @Test
    void htmlKindPassesThrough() {
        String html = HoverHtmlRenderer.render(List.of(new HoverContent("html", "<p>ready</p>")));

        assertTrue(html.contains("<p>ready</p>"));
    }

    @Test
    void blankContentsAreSkipped() {
        assertEquals("", HoverHtmlRenderer.render(List.of(new HoverContent("plaintext", " "))));
    }

    @Test
    void toHtmlWrapsResultAsSingleHtmlContent() {
        HoverResult result = new HoverResult(List.of(new HoverContent("markdown", "hello")), 4, 9);

        Optional<HoverResult> html = HoverHtmlRenderer.toHtml(result);

        assertTrue(html.isPresent());
        assertEquals(1, html.get().contents().size());
        assertEquals("html", html.get().contents().getFirst().kind());
        assertTrue(html.get().contents().getFirst().value().contains("hello"));
        assertEquals(4, html.get().rangeStart());
        assertEquals(9, html.get().rangeEnd());
    }

    @Test
    void toHtml_emptyOrBlank_returnsEmpty() {
        assertTrue(HoverHtmlRenderer.toHtml(null).isEmpty());
        assertTrue(HoverHtmlRenderer.toHtml(new HoverResult(List.of(), 0, 0)).isEmpty());
        assertTrue(HoverHtmlRenderer.toHtml(
                new HoverResult(List.of(new HoverContent("plaintext", "")), 0, 0)).isEmpty());
    }
}
