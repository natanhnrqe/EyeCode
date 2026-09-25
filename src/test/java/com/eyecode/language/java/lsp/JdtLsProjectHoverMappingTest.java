package com.eyecode.language.java.lsp;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.MarkupContent;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdtLsProjectHoverMappingTest {
    @Test
    void markupContentMapsToContentsAndRange() {
        Hover hover = new Hover(new MarkupContent("markdown", "**campo**"));
        hover.setRange(new Range(new Position(0, 1), new Position(0, 4)));

        var result = JdtLsProjectCompletion.toHoverResult(hover, request("abcdef", 4));

        assertEquals(1, result.contents().size());
        assertEquals("markdown", result.contents().getFirst().kind());
        assertEquals("**campo**", result.contents().getFirst().value());
        assertEquals(1, result.rangeStart());
        assertEquals(4, result.rangeEnd());
    }

    @Test
    void rangelessHoverFallsBackToCaretOffset() {
        Hover hover = new Hover(new MarkupContent("plaintext", "docs"));

        var result = JdtLsProjectCompletion.toHoverResult(hover, request("abcdef", 4));

        assertEquals(4, result.rangeStart());
        assertEquals(4, result.rangeEnd());
        assertEquals("docs", result.contents().getFirst().value());
    }

    @Test
    void markedStringListMapsToMultipleContents() {
        Either<String, MarkedString> plain = Either.<String, MarkedString>forLeft("texto puro");
        Either<String, MarkedString> code = Either.<String, MarkedString>forRight(new MarkedString("java", "int x"));
        Hover hover = new Hover(List.of(plain, code));
        hover.setRange(new Range(new Position(0, 0), new Position(0, 6)));

        var result = JdtLsProjectCompletion.toHoverResult(hover, request("abcdef", 0));

        assertEquals(2, result.contents().size());
        assertEquals("plaintext", result.contents().getFirst().kind());
        assertEquals("texto puro", result.contents().getFirst().value());
        assertEquals("java", result.contents().get(1).kind());
        assertEquals("int x", result.contents().get(1).value());
        assertEquals(0, result.rangeStart());
        assertEquals(6, result.rangeEnd());
    }

    @Test
    void nullContentsProduceEmptyResultAtCaret() {
        var result = JdtLsProjectCompletion.toHoverResult(new Hover(), request("abcdef", 3));

        assertTrue(result.contents().isEmpty());
        assertEquals(3, result.rangeStart());
        assertEquals(3, result.rangeEnd());
    }

    @Test
    void sanitizeMarkupRemovesScriptBlocks() {
        assertEquals("<p>a</p><p>b</p>", JdtLsProjectCompletion.sanitizeMarkup(
                "<p>a</p><SCRIPT type=\"text/javascript\">\nevil();\n</SCRIPT><p>b</p>"));
    }

    @Test
    void sanitizeMarkupRemovesDangerousSchemesAndEventHandlers() {
        String sanitized = JdtLsProjectCompletion.sanitizeMarkup(
                "<a href=\"javascript:alert(1)\" onclick=\"steal()\" x=\"https://example.com\">y</a>");

        assertFalse(sanitized.contains("javascript:"));
        assertFalse(sanitized.toLowerCase().contains("onclick"));
        assertTrue(sanitized.contains("https://example.com"));
    }

    @Test
    void sanitizeMarkupPreservesCodeElements() {
        assertEquals("<code>int on = 1;</code>",
                JdtLsProjectCompletion.sanitizeMarkup("<code>int on = 1;</code>"));
        assertEquals("", JdtLsProjectCompletion.sanitizeMarkup(null));
    }

    private static LanguageFeatureRequest request(String source, int caretOffset) {
        return new LanguageFeatureRequest(
                new LanguageDocument("file:///Main.java", null, "Main.java", LanguageId.JAVA),
                1, source, caretOffset);
    }
}
