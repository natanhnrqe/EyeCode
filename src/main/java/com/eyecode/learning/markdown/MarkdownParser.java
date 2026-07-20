package com.eyecode.learning.markdown;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MarkdownParser {

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.+?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.+?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^)]+)\\)");

    public MarkdownDocument parse(String markdown) {
        List<MarkdownNode> nodes = new ArrayList<>();
        if (markdown == null || markdown.isBlank()) {
            return new MarkdownDocument(nodes);
        }

        String[] lines = markdown.split("\n", -1);
        int i = 0;
        while (i < lines.length) {
            String line = lines[i];

            if (line.isBlank()) {
                i++;
                continue;
            }

            if (line.startsWith("```")) {
                i = parseCodeBlock(lines, i, nodes);
            } else if (line.startsWith("# ")) {
                nodes.add(new HeadingNode(1, line.substring(2).trim()));
                i++;
            } else if (line.startsWith("## ")) {
                nodes.add(new HeadingNode(2, line.substring(3).trim()));
                i++;
            } else if (line.trim().equals("---")) {
                nodes.add(new DividerNode());
                i++;
            } else if (line.trim().startsWith("- ") || line.trim().startsWith("\u2022 ")) {
                i = parseBulletLines(lines, i, nodes);
            } else if (line.trim().equals("\u2193")) {
                nodes.add(new ParagraphNode(List.of(Segment.text("\u2193"))));
                i++;
            } else {
                i = parseParagraph(lines, i, nodes);
            }
        }

        return new MarkdownDocument(nodes);
    }

    private int parseCodeBlock(String[] lines, int start, List<MarkdownNode> nodes) {
        String firstLine = lines[start].trim();
        String language = firstLine.length() > 3 ? firstLine.substring(3).trim() : "";
        StringBuilder code = new StringBuilder();
        int i = start + 1;
        while (i < lines.length) {
            String line = lines[i];
            if (line.trim().equals("```")) {
                i++;
                break;
            }
            if (code.length() > 0) {
                code.append("\n");
            }
            code.append(line);
            i++;
        }
        nodes.add(new CodeBlockNode(language, code.toString()));
        return i;
    }

    private int parseBulletLines(String[] lines, int start, List<MarkdownNode> nodes) {
        int i = start;
        while (i < lines.length) {
            String trimmed = lines[i].trim();
            if (trimmed.startsWith("- ")) {
                nodes.add(new BulletNode(trimmed.substring(2).trim(), false));
                i++;
            } else if (trimmed.startsWith("\u2022 ")) {
                nodes.add(new BulletNode(trimmed.substring(2).trim(), false));
                i++;
            } else {
                break;
            }
        }
        return i;
    }

    private int parseParagraph(String[] lines, int start, List<MarkdownNode> nodes) {
        StringBuilder text = new StringBuilder();
        int i = start;
        while (i < lines.length) {
            String line = lines[i];
            if (line.isBlank()) {
                break;
            }
            String trimmed = line.trim();
            if (trimmed.startsWith("```")
                    || trimmed.startsWith("#") || trimmed.equals("---")) {
                break;
            }
            if (text.length() > 0) {
                text.append(" ");
            }
            text.append(trimmed);
            i++;
        }
        String result = text.toString().trim();
        if (!result.isEmpty()) {
            nodes.add(new ParagraphNode(parseInline(result)));
        }
        return i;
    }

    static List<Segment> parseInline(String text) {
        if (text == null || text.isBlank()) {
            return List.of(Segment.text(text != null ? text : ""));
        }

        List<Segment> segments = new ArrayList<>();
        int pos = 0;

        while (pos < text.length()) {
            Matcher linkMatcher = LINK_PATTERN.matcher(text).region(pos, text.length());
            if (linkMatcher.find() && linkMatcher.start() == pos) {
                segments.add(Segment.link(linkMatcher.group(1), linkMatcher.group(2)));
                pos = linkMatcher.end();
                continue;
            }

            Matcher boldMatcher = BOLD_PATTERN.matcher(text).region(pos, text.length());
            if (boldMatcher.find() && boldMatcher.start() == pos) {
                segments.add(Segment.bold(boldMatcher.group(1)));
                pos = boldMatcher.end();
                continue;
            }

            Matcher italicMatcher = ITALIC_PATTERN.matcher(text).region(pos, text.length());
            if (italicMatcher.find() && italicMatcher.start() == pos) {
                segments.add(Segment.italic(italicMatcher.group(1)));
                pos = italicMatcher.end();
                continue;
            }

            Matcher codeMatcher = CODE_PATTERN.matcher(text).region(pos, text.length());
            if (codeMatcher.find() && codeMatcher.start() == pos) {
                segments.add(Segment.code(codeMatcher.group(1)));
                pos = codeMatcher.end();
                continue;
            }

            int nextPos = findNextSpecial(text, pos + 1);
            if (nextPos < 0) {
                segments.add(Segment.text(text.substring(pos)));
                pos = text.length();
            } else {
                segments.add(Segment.text(text.substring(pos, nextPos)));
                pos = nextPos;
            }
        }

        return segments;
    }

    private static int findNextSpecial(String text, int from) {
        for (int i = from; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '*' || c == '`' || c == '[') {
                return i;
            }
        }
        return -1;
    }
}
