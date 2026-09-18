package com.eyecode.lessons.presentation;

public record CodeChange(CodeChangeKind kind, int startOffset, int endOffset, String insertedText) {
    public CodeChange {
        if (kind == null || startOffset < 0 || endOffset < startOffset || insertedText == null) {
            throw new IllegalArgumentException("Alteração de apresentação inválida");
        }
    }
}
