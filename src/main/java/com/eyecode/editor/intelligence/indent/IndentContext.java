package com.eyecode.editor.intelligence.indent;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;

/**
 * Single-pass lexical scan of a snapshot that tracks Java bracket nesting while
 * ignoring brackets inside string literals, character literals, line comments
 * and block comments.
 * <p>
 * The scan records, per line, the first and last significant character (any
 * non-whitespace token outside comments and literals) and the net {@code {}},
 * {@code ()} and {@code []} deltas. Cumulative depths are derived so a policy
 * can ask "what is the block nesting level at the start of line N" in O(1).
 * <p>
 * This is a lexical helper, not a parser: it does not validate that brackets
 * match and does not understand text blocks (triple-quoted strings) — those
 * remain a parser concern.
 */
public final class IndentContext {

    private final int lineCount;
    private final int[] firstChar;
    private final int[] lastChar;
    private final int[] netBrace;
    private final int[] netParen;
    private final int[] netBracket;
    private final int[] blockDepthAtLineStart;
    private final int[] parenDepthAtLineStart;
    private final boolean[] inBlockComment;
    private final boolean[] inString;

    private IndentContext(int lineCount,
                          int[] firstChar,
                          int[] lastChar,
                          int[] netBrace,
                          int[] netParen,
                          int[] netBracket,
                          int[] blockDepthAtLineStart,
                          int[] parenDepthAtLineStart,
                          boolean[] inBlockComment,
                          boolean[] inString) {
        this.lineCount = lineCount;
        this.firstChar = firstChar;
        this.lastChar = lastChar;
        this.netBrace = netBrace;
        this.netParen = netParen;
        this.netBracket = netBracket;
        this.blockDepthAtLineStart = blockDepthAtLineStart;
        this.parenDepthAtLineStart = parenDepthAtLineStart;
        this.inBlockComment = inBlockComment;
        this.inString = inString;
    }

    public static IndentContext of(DocumentSnapshot snapshot) {
        return of(snapshot == null ? "" : snapshot.getText());
    }

    public static IndentContext of(CharSequence text) {
        return new Scanner(text == null ? "" : text.toString()).scan();
    }

    public int lineCount() {
        return lineCount;
    }

    public int blockDepthAtLineStart(int line) {
        return blockDepthAtLineStart[line];
    }

    public int parenDepthAtLineStart(int line) {
        return parenDepthAtLineStart[line];
    }

    public int lineNetBraceDelta(int line) {
        return netBrace[line];
    }

    public int lineNetParenDelta(int line) {
        return netParen[line];
    }

    public int lineNetBracketDelta(int line) {
        return netBracket[line];
    }

    public boolean lineEndsWithOpenBrace(int line) {
        return lastChar[line] == '{';
    }

    public boolean lineEndsWithOpenParen(int line) {
        return lastChar[line] == '(';
    }

    public boolean lineEndsWithComma(int line) {
        return lastChar[line] == ',';
    }

    public boolean lineStartsWithClosingBrace(int line) {
        return firstChar[line] == '}';
    }

    public boolean lineIsBlank(int line) {
        return firstChar[line] == 0 && lastChar[line] == 0;
    }

    public boolean inBlockComment(int line) {
        return inBlockComment[line];
    }

    public boolean inString(int line) {
        return inString[line];
    }

    private static final class Scanner {

        private static final int NORMAL = 0;
        private static final int LINE_COMMENT = 1;
        private static final int BLOCK_COMMENT = 2;
        private static final int STRING_DOUBLE = 3;
        private static final int STRING_SINGLE = 4;

        private final String text;
        private final int[] firstChar;
        private final int[] lastChar;
        private final int[] netBrace;
        private final int[] netParen;
        private final int[] netBracket;
        private final int[] blockDepthAtLineStart;
        private final int[] parenDepthAtLineStart;
        private final boolean[] inBlockComment;
        private final boolean[] inString;

        Scanner(String text) {
            this.text = text;
            int lines = countLines(text);
            firstChar = new int[lines];
            lastChar = new int[lines];
            netBrace = new int[lines];
            netParen = new int[lines];
            netBracket = new int[lines];
            blockDepthAtLineStart = new int[lines];
            parenDepthAtLineStart = new int[lines];
            inBlockComment = new boolean[lines];
            inString = new boolean[lines];
        }

        IndentContext scan() {
            int line = 0;
            int state = NORMAL;
            int first = 0;
            int last = 0;
            int block = 0;
            int paren = 0;
            int bracket = 0;
            int length = text.length();
            for (int i = 0; i < length; i++) {
                char c = text.charAt(i);
                if (c == '\n' || c == '\r') {
                    firstChar[line] = first;
                    lastChar[line] = last;
                    if (c == '\r' && i + 1 < length && text.charAt(i + 1) == '\n') {
                        i++;
                    }
                    line++;
                    if (line < lineCount()) {
                        blockDepthAtLineStart[line] = block;
                        parenDepthAtLineStart[line] = paren;
                        inBlockComment[line] = state == BLOCK_COMMENT;
                        inString[line] = state == STRING_DOUBLE || state == STRING_SINGLE;
                    }
                    first = 0;
                    last = 0;
                    if (state == LINE_COMMENT) {
                        state = NORMAL;
                    }
                    continue;
                }
                switch (state) {
                    case LINE_COMMENT -> {
                    }
                    case BLOCK_COMMENT -> {
                        if (c == '*' && i + 1 < length && text.charAt(i + 1) == '/') {
                            state = NORMAL;
                            i++;
                        }
                    }
                    case STRING_DOUBLE -> {
                        if (c == '\\') {
                            i++;
                        } else if (c == '"') {
                            state = NORMAL;
                        }
                    }
                    case STRING_SINGLE -> {
                        if (c == '\\') {
                            i++;
                        } else if (c == '\'') {
                            state = NORMAL;
                        }
                    }
                    default -> {
                        if (c == '/' && i + 1 < length && text.charAt(i + 1) == '/') {
                            state = LINE_COMMENT;
                            i++;
                        } else if (c == '/' && i + 1 < length && text.charAt(i + 1) == '*') {
                            state = BLOCK_COMMENT;
                            i++;
                        } else if (c == '"') {
                            state = STRING_DOUBLE;
                            if (first == 0) first = c;
                            last = c;
                        } else if (c == '\'') {
                            state = STRING_SINGLE;
                            if (first == 0) first = c;
                            last = c;
                        } else if (c == '{' || c == '}') {
                            netBrace[line] += c == '{' ? 1 : -1;
                            block += c == '{' ? 1 : -1;
                            if (first == 0) first = c;
                            last = c;
                        } else if (c == '(' || c == ')') {
                            netParen[line] += c == '(' ? 1 : -1;
                            paren += c == '(' ? 1 : -1;
                            if (first == 0) first = c;
                            last = c;
                        } else if (c == '[' || c == ']') {
                            netBracket[line] += c == '[' ? 1 : -1;
                            bracket += c == '[' ? 1 : -1;
                            if (first == 0) first = c;
                            last = c;
                        } else if (!Character.isWhitespace(c)) {
                            if (first == 0) first = c;
                            last = c;
                        }
                    }
                }
            }
            firstChar[line] = first;
            lastChar[line] = last;
            return new IndentContext(
                    lineCount(),
                    firstChar, lastChar,
                    netBrace, netParen, netBracket,
                    blockDepthAtLineStart, parenDepthAtLineStart,
                    inBlockComment, inString
            );
        }

        private int lineCount() {
            return firstChar.length;
        }
    }

    private static int countLines(CharSequence text) {
        int lines = 1;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\r') {
                lines++;
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
            }
        }
        return lines;
    }
}
