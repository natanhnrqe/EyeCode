package com.eyecode.language.hover;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;

import java.util.List;
import java.util.Optional;

public final class HoverHtmlRenderer {
    private static final Parser MARKDOWN_PARSER = Parser.builder().build();
    private static final HtmlRenderer MARKDOWN_RENDERER = HtmlRenderer.builder().build();

    private HoverHtmlRenderer() {
    }

    public static Optional<HoverResult> toHtml(HoverResult result) {
        if (result == null || result.contents().isEmpty()) {
            return Optional.empty();
        }
        String html = render(result.contents());
        if (html.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(new HoverResult(List.of(new HoverContent("html", html)),
                result.rangeStart(), result.rangeEnd()));
    }

    public static String render(List<HoverContent> contents) {
        StringBuilder body = new StringBuilder();
        for (HoverContent content : contents) {
            if (content.value().isBlank()) {
                continue;
            }
            switch (content.kind()) {
                case "java" -> body.append("<pre><code class=\"language-java\">")
                        .append(escape(content.value()))
                        .append("</code></pre>");
                case "markdown" -> body.append(MARKDOWN_RENDERER.render(MARKDOWN_PARSER.parse(content.value())));
                case "html" -> body.append(content.value());
                default -> body.append("<p>").append(escape(content.value())).append("</p>");
            }
        }
        if (body.isEmpty()) {
            return "";
        }
        return "<div class=\"hover-doc\">" + body + "</div>";
    }

    private static String escape(String value) {
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
