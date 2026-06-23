package com.eyecode.editor.v2.syntax;

public record SyntaxToken(
        TokenType type,
        int startOffset,
        int endOffset,
        String text
) {
}
