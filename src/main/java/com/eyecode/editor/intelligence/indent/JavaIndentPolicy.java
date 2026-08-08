package com.eyecode.editor.intelligence.indent;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;

/**
 * Java indentation rules on top of {@link IndentContext}.
 * <p>
 * The next-line level is computed canonically from bracket nesting: a new line
 * following line N sits at the block nesting level that line N leaves behind,
 * which automatically handles {@code {}} blocks, closing braces, {@code } else {}}
 * chains and {@code case}/{@code default} labels, and ignores braces that live
 * inside strings, character literals or comments (as reported by the context
 * scan). When a line carries no structural tokens at all, the level falls back
 * to the line's current leading indentation so plain statements keep their
 * existing indent.
 * <p>
 * Continuation rules add one level when a line ends with {@code (} or {@code ,}
 * so multi-line call chains and initializers keep their arguments indented.
 */
public final class JavaIndentPolicy implements IndentPolicy {

    public static final JavaIndentPolicy INSTANCE = new JavaIndentPolicy();

    @Override
    public int indentSize() {
        return 4;
    }

    @Override
    public String indentationFor(int level) {
        if (level <= 0) {
            return "";
        }
        return "    ".repeat(level);
    }

    @Override
    public int indentationLevel(String text, int line) {
        if (text == null || text.isEmpty() || line < 0) {
            return 0;
        }
        int lineStart = lineStartOffset(text, line);
        int level = 0;
        int spaces = 0;
        for (int i = lineStart; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == ' ') {
                spaces++;
            } else if (c == '\t') {
                level++;
            } else {
                break;
            }
        }
        return level + spaces / indentSize();
    }

    @Override
    public int currentLineIndentLevel(DocumentSnapshot snapshot, int line) {
        return indentationLevel(snapshot == null ? "" : snapshot.getText(), line);
    }

    @Override
    public int nextLineIndentLevel(DocumentSnapshot snapshot, int line) {
        if (snapshot == null || line < 0) {
            return 0;
        }
        IndentContext context = IndentContext.of(snapshot);
        int safeLine = Math.min(line, context.lineCount() - 1);
        int level = context.blockDepthAtLineStart(safeLine) + context.lineNetBraceDelta(safeLine);
        boolean continuation = context.lineEndsWithOpenParen(safeLine) || context.lineEndsWithComma(safeLine);
        if (continuation) {
            level++;
        }
        boolean switchLabel = isSwitchLabel(snapshot.getText(), safeLine);
        if (switchLabel) {
            level++;
        }
        if (!switchLabel
                && !continuation
                && context.lineNetBraceDelta(safeLine) == 0
                && context.lineNetParenDelta(safeLine) == 0
                && context.lineNetBracketDelta(safeLine) == 0) {
            level = Math.max(level, currentLineIndentLevel(snapshot, safeLine));
        }
        return Math.max(0, level);
    }

    @Override
    public boolean shouldDedent(DocumentSnapshot snapshot, int line) {
        if (snapshot == null || line < 0) {
            return false;
        }
        IndentContext context = IndentContext.of(snapshot);
        return line < context.lineCount() && context.lineStartsWithClosingBrace(line);
    }

    private static boolean isSwitchLabel(String text, int line) {
        int lineStart = lineStartOffset(text, line);
        int i = lineStart;
        while (i < text.length() && Character.isWhitespace(text.charAt(i))) {
            i++;
        }
        if (text.startsWith("case", i)
                && (i + 4 == text.length() || !Character.isLetterOrDigit(text.charAt(i + 4)))) {
            return true;
        }
        return text.startsWith("default", i)
                && (i + 7 == text.length() || !Character.isLetterOrDigit(text.charAt(i + 7)));
    }

    private static int lineStartOffset(String text, int line) {
        if (line <= 0) {
            return 0;
        }
        int current = 0;
        int index = 0;
        while (current < line && index < text.length()) {
            char c = text.charAt(index++);
            if (c == '\n') {
                current++;
            } else if (c == '\r') {
                if (index < text.length() && text.charAt(index) == '\n') {
                    index++;
                }
                current++;
            }
        }
        return index;
    }
}
