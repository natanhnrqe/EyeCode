package com.eyecode.language.java;

import com.eyecode.language.TokenType;

public enum JavaTokenType implements TokenType {

    EOF,
    ERROR,
    WHITESPACE,
    COMMENT,
    KEYWORD,
    IDENTIFIER,
    NUMBER,
    STRING,
    CHARACTER,
    OPERATOR,
    SEPARATOR,
    AT,
    BOOLEAN_LITERAL,
    NULL_LITERAL
}
