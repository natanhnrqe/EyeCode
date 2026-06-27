package com.eyecode.editor.v2.syntax;

import java.util.HashMap;
import java.util.Map;

public final class DocumentStyleRegistry {

    private final Map<String, StyleDefinition> styles = new HashMap<>();
    private final Map<TokenType, String> tokenStyles = new HashMap<>();

    public DocumentStyleRegistry() {
        registerDefault(TokenType.KEYWORD, new StyleDefinition("keyword", false, false, "keyword"));
        registerDefault(TokenType.IDENTIFIER, new StyleDefinition("identifier", false, false, "identifier"));
        registerDefault(TokenType.UNKNOWN, new StyleDefinition("unknown", false, false, "unknown"));
        registerDefault(TokenType.WHITESPACE, new StyleDefinition("whitespace", false, false, "whitespace"));
        registerDefault(TokenType.STRING, new StyleDefinition("string", false, false, "string"));
        registerDefault(TokenType.COMMENT, new StyleDefinition("comment", false, true, "comment"));
        registerDefault(TokenType.NUMBER, new StyleDefinition("number", false, false, "number"));
        registerDefault(TokenType.ANNOTATION, new StyleDefinition("annotation", false, false, "annotation"));
        registerDefault(TokenType.OPERATOR, new StyleDefinition("operator", false, false, "operator"));
        registerDefault(TokenType.SEPARATOR, new StyleDefinition("separator", false, false, "separator"));
    }

    public void register(String name, StyleDefinition definition) {
        if (name == null || definition == null) return;
        styles.put(name, definition);
    }

    public StyleDefinition get(String name) {
        return styles.get(name);
    }

    public StyleDefinition getFor(TokenType tokenType) {
        String styleName = tokenStyles.get(tokenType);
        if (styleName == null) return styles.get("unknown");
        return styles.getOrDefault(styleName, styles.get("unknown"));
    }

    private void registerDefault(TokenType tokenType, StyleDefinition definition) {
        register(definition.name(), definition);
        tokenStyles.put(tokenType, definition.name());
    }
}
