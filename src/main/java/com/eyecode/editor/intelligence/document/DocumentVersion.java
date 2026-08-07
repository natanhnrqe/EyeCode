package com.eyecode.editor.intelligence.document;

public record DocumentVersion(long sequence) {

    public static final DocumentVersion ZERO = new DocumentVersion(0L);

    public DocumentVersion {
        if (sequence < 0) {
            throw new IllegalArgumentException("sequence < 0: " + sequence);
        }
    }

    public boolean isAfter(DocumentVersion other) {
        return other != null && sequence > other.sequence;
    }

    public boolean isBefore(DocumentVersion other) {
        return other != null && sequence < other.sequence;
    }

    public DocumentVersion next() {
        return new DocumentVersion(sequence + 1);
    }
}
