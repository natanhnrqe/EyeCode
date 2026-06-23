package com.eyecode.editor.v2.syntax;

import java.util.EnumMap;
import java.util.Map;

public final class SyntaxTheme {

    private final Map<TokenType, String> styles;

    public SyntaxTheme() {
        this.styles = new EnumMap<>(TokenType.class);
        styles.put(TokenType.KEYWORD, "keyword");
        styles.put(TokenType.IDENTIFIER, "identifier");
        styles.put(TokenType.STRING, "string");
        styles.put(TokenType.NUMBER, "number");
        styles.put(TokenType.COMMENT, "comment");
        styles.put(TokenType.ANNOTATION, "annotation");
        styles.put(TokenType.OPERATOR, "operator");
        styles.put(TokenType.SEPARATOR, "separator");
        styles.put(TokenType.WHITESPACE, "whitespace");
        styles.put(TokenType.UNKNOWN, "unknown");
    }

    public String getStyle(TokenType type) {
        return styles.getOrDefault(type, "unknown");
    }

    public Map<TokenType, String> getStyles() {
        return Map.copyOf(styles);
    }
}
