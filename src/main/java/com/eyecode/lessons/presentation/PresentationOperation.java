package com.eyecode.lessons.presentation;

public record PresentationOperation(PresentationOperationType type, int startOffset, int endOffset,
                                    String prefix, String text, String suffix) {
    public PresentationOperation {
        if (type == null || startOffset < 0 || endOffset < startOffset || prefix == null || text == null || suffix == null) {
            throw new IllegalArgumentException("Operação de apresentação inválida");
        }
    }

    public static PresentationOperation edit(PresentationOperationType type, int startOffset, int endOffset, String text) {
        return new PresentationOperation(type, startOffset, endOffset, "", text, "");
    }

    public static PresentationOperation typeLine(int startOffset, int endOffset, String prefix, String text, String suffix) {
        return new PresentationOperation(PresentationOperationType.TYPE_TEXT, startOffset, endOffset, prefix, text, suffix);
    }

    public static PresentationOperation materialize(int sourceLength, String target) {
        return edit(PresentationOperationType.MATERIALIZE, 0, sourceLength, target);
    }
}
