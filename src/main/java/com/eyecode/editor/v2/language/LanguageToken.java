package com.eyecode.editor.v2.language;

import com.eyecode.editor.v2.syntax.TokenType;

import java.util.Objects;

public final class LanguageToken {

    private final TokenType type;
    private final String text;
    private final int startOffset;
    private final int endOffset;

    public LanguageToken(TokenType type, String text, int startOffset, int endOffset) {
        this.type = Objects.requireNonNull(type);
        this.text = Objects.requireNonNull(text);
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    public TokenType getType() { return type; }

    public String getText() { return text; }

    public int getStartOffset() { return startOffset; }

    public int getEndOffset() { return endOffset; }
}
