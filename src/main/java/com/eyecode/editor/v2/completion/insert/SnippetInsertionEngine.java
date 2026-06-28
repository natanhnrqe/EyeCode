package com.eyecode.editor.v2.completion.insert;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;

public final class SnippetInsertionEngine {

    private static final String CARET_MARKER = "${0}";
    private static final String INDENTATION_UNIT = "    ";

    public record SnippetResult(String text, int caretOffset) {}

    public SnippetResult insert(EditorDocument document, EditorPosition caret, String currentPrefix, String snippetText) {
        String text = document.getText();
        int caretOffset = toOffset(text, caret);
        int start = Math.max(0, caretOffset - currentPrefix.length());
        int end = caretOffset;

        String baseIndent = leadingIndent(text, start);

        String expanded = expandSnippet(snippetText, baseIndent);
        int markerPos = expanded.indexOf(CARET_MARKER);
        String finalText;
        int finalCaret;

        if (markerPos >= 0) {
            finalText = expanded.substring(0, markerPos) + expanded.substring(markerPos + CARET_MARKER.length());
            finalCaret = start + markerPos;
        } else {
            finalText = expanded;
            finalCaret = start + finalText.length();
        }

        if (end > start) {
            document.delete(start, end);
        }
        document.insert(start, finalText);

        return new SnippetResult(finalText, finalCaret);
    }

    private String expandSnippet(String snippet, String baseIndent) {
        String[] lines = snippet.split("\n", -1);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i == 0) {
                sb.append(lines[i]);
            } else {
                sb.append("\n");
                if (!lines[i].isEmpty()) {
                    sb.append(baseIndent).append(lines[i]);
                }
            }
        }
        return sb.toString();
    }

    private String leadingIndent(String text, int offset) {
        int lineStart = findLineStart(text, offset);
        int end = lineStart;
        while (end < text.length()) {
            char c = text.charAt(end);
            if (c != ' ' && c != '\t') break;
            end++;
        }
        return text.substring(lineStart, end);
    }

    private int findLineStart(String text, int offset) {
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int lineStart = text.lastIndexOf('\n', Math.max(0, safeOffset - 1));
        return lineStart < 0 ? 0 : lineStart + 1;
    }

    private int toOffset(String text, EditorPosition position) {
        int line = 0;
        int column = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            if (line == position.line() && column == position.column()) {
                return offset;
            }
            char current = text.charAt(offset);
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return text.length();
    }
}
