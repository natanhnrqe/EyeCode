package com.eyecode.editor.v2.syntax;

import com.eyecode.language.java.JavaTokenType;

public final class SyntaxTokenTypeMapper {

    private SyntaxTokenTypeMapper() {
    }

    public static TokenType map(JavaTokenType type) {
        return switch (type) {
            case KEYWORD -> TokenType.KEYWORD;
            case IDENTIFIER -> TokenType.IDENTIFIER;
            case STRING -> TokenType.STRING;
            case CHARACTER -> TokenType.STRING;
            case NUMBER -> TokenType.NUMBER;
            case COMMENT -> TokenType.COMMENT;
            case OPERATOR -> TokenType.OPERATOR;
            case SEPARATOR -> TokenType.SEPARATOR;
            case WHITESPACE -> TokenType.WHITESPACE;
            case BOOLEAN_LITERAL, NULL_LITERAL -> TokenType.KEYWORD;
            case ERROR, AT -> TokenType.UNKNOWN;
            case EOF -> null;
        };
    }
}
