package com.eyecode.editor.v2.syntax;

import java.util.List;

public final class SyntaxSnapshot {

    private final List<SyntaxToken> tokens;
    private final long createdAt;

    public SyntaxSnapshot(List<SyntaxToken> tokens) {
        this.tokens = List.copyOf(tokens);
        this.createdAt = System.currentTimeMillis();
    }

    public List<SyntaxToken> getTokens() {
        return tokens;
    }

    public List<SyntaxToken> getTokensByType(TokenType type) {
        return tokens.stream()
                .filter(token -> token.type() == type)
                .toList();
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    public long getCreatedAt() {
        return createdAt;
    }
}
