package com.eyecode.editor.intelligence.pipeline.strategy;

/**
 * Static tables and predicates for the delimiter pairs handled by smart editing.
 * <p>
 * Pure Core: no Swing, JavaFX or AWT references. Pairs:
 * {@code ( )}, {@code [ ]}, {@code { }}, {@code " "} and {@code ' '}.
 */
final class Delimiters {

    private Delimiters() {
    }

    static char closingFor(char opening) {
        return switch (opening) {
            case '(' -> ')';
            case '[' -> ']';
            case '{' -> '}';
            case '"' -> '"';
            case '\'' -> '\'';
            default -> '\0';
        };
    }

    static boolean isOpening(char character) {
        return character == '(' || character == '[' || character == '{';
    }

    static boolean isClosing(char character) {
        return character == ')' || character == ']' || character == '}'
                || character == '"' || character == '\'';
    }

    static boolean isQuote(char character) {
        return character == '"' || character == '\'';
    }
}
