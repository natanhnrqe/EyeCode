package com.eyecode.editor.intelligence.selection;

/**
 * One-pass lexical mask for Java selection heuristics.
 * <p>
 * Marks every offset that belongs to code (as opposed to a string literal, a
 * character literal, a line comment or a block comment). Bracket matching and
 * boundary walks in {@link JavaSelectionExpander} consult this mask so
 * delimiters inside literals and comments are never treated as structure.
 * <p>
 * The scan shares the lexical states of {@code IndentContext} (line comment,
 * block comment, double/single-quoted strings with {@code \} escapes) and
 * deliberately knows nothing about text blocks, so a triple-quoted string is
 * misclassified — the same documented limitation as the indent scanner.
 */
final class SelectionLexicon {

    private static final int NORMAL = 0;
    private static final int LINE_COMMENT = 1;
    private static final int BLOCK_COMMENT = 2;
    private static final int STRING_DOUBLE = 3;
    private static final int STRING_SINGLE = 4;

    private SelectionLexicon() {
    }

    static boolean[] codeMask(CharSequence text) {
        int length = text.length();
        boolean[] code = new boolean[length];
        int state = NORMAL;
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            switch (state) {
                case NORMAL -> {
                    if (c == '/' && i + 1 < length && text.charAt(i + 1) == '/') {
                        state = LINE_COMMENT;
                        i++;
                    } else if (c == '/' && i + 1 < length && text.charAt(i + 1) == '*') {
                        state = BLOCK_COMMENT;
                        i++;
                    } else if (c == '"') {
                        state = STRING_DOUBLE;
                    } else if (c == '\'') {
                        state = STRING_SINGLE;
                    } else {
                        code[i] = true;
                    }
                }
                case LINE_COMMENT -> {
                    if (c == '\n' || c == '\r') {
                        state = NORMAL;
                    }
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
                }
            }
        }
        return code;
    }
}
