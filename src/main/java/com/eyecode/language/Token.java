package com.eyecode.language;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Objects;

public record Token(TokenType type, TextRange range, String text) {

    public Token {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(range, "range");
        Objects.requireNonNull(text, "text");
    }

    public int startOffset() {
        return range.startOffset();
    }

    public int endOffset() {
        return range.endOffset();
    }

    public int length() {
        return range.length();
    }
}
