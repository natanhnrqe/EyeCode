package com.eyecode.learning.html;

import com.eyecode.learning.markdown.*;
import java.util.List;

@Deprecated(forRemoval = true)
public final class MarkdownToHtmlConverter {

    public String convert(MarkdownDocument document) {
        if (document == null) {
            return "";
        }
        var body = new StringBuilder();
        var nodes = document.nodes();
        int i = 0;
        while (i < nodes.size()) {
            var node = nodes.get(i);
            if (node instanceof BulletNode) {
                i = appendBulletList(body, nodes, i);
            } else {
                appendNode(body, node);
                i++;
            }
        }
        return wrapDocument(body.toString());
    }

    private int appendBulletList(StringBuilder body, List<MarkdownNode> nodes, int start) {
        body.append("<ul>").append(System.lineSeparator());
        int i = start;
        while (i < nodes.size() && nodes.get(i) instanceof BulletNode bullet) {
            body.append("<li>").append(escapeHtml(bullet.text())).append("</li>")
                    .append(System.lineSeparator());
            i++;
        }
        body.append("</ul>").append(System.lineSeparator());
        return i;
    }

    private void appendNode(StringBuilder body, MarkdownNode node) {
        switch (node) {
            case HeadingNode h -> {
                String tag = "h" + h.level();
                body.append("<").append(tag).append(">")
                        .append(escapeHtml(h.text()))
                        .append("</").append(tag).append(">")
                        .append(System.lineSeparator());
            }
            case ParagraphNode p -> {
                body.append("<p>");
                for (var seg : p.segments()) {
                    appendSegment(body, seg);
                }
                body.append("</p>").append(System.lineSeparator());
            }
            case CodeBlockNode cb -> {
                String lang = cb.language() != null ? cb.language() : "";
                body.append("<pre><code");
                if (!lang.isEmpty()) {
                    body.append(" class=\"language-").append(escapeHtml(lang)).append("\"");
                }
                body.append(">").append(escapeHtml(cb.code()))
                        .append("</code></pre>").append(System.lineSeparator());
            }
            case DividerNode d -> {
                body.append("<hr>").append(System.lineSeparator());
            }
            default -> {}
        }
    }

    private void appendSegment(StringBuilder body, Segment seg) {
        String text = escapeHtml(seg.text());
        switch (seg.type()) {
            case BOLD -> body.append("<strong>").append(text).append("</strong>");
            case ITALIC -> body.append("<em>").append(text).append("</em>");
            case CODE -> body.append("<code>").append(text).append("</code>");
            case LINK -> {
                String url = escapeHtml(seg.url());
                body.append("<a href=\"").append(url).append("\">").append(text).append("</a>");
            }
            default -> body.append(text);
        }
    }

    private String wrapDocument(String bodyContent) {
        return "<!DOCTYPE html>" + System.lineSeparator()
                + "<html>" + System.lineSeparator()
                + "<head>" + System.lineSeparator()
                + "<meta charset=\"UTF-8\">" + System.lineSeparator()
                + "</head>" + System.lineSeparator()
                + "<body>" + System.lineSeparator()
                + bodyContent
                + "</body>" + System.lineSeparator()
                + "</html>" + System.lineSeparator();
    }

    private String escapeHtml(String input) {
        if (input == null || input.isEmpty()) {
            return input != null ? input : "";
        }
        var escaped = new StringBuilder(input.length());
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            switch (c) {
                case '&' -> escaped.append("&amp;");
                case '<' -> escaped.append("&lt;");
                case '>' -> escaped.append("&gt;");
                case '"' -> escaped.append("&quot;");
                default -> escaped.append(c);
            }
        }
        return escaped.toString();
    }
}
