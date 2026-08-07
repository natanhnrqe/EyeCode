package com.eyecode.editor.intelligence.document;

public record TextRange(int startOffset, int endOffset) {

    public TextRange {
        if (startOffset < 0) {
            throw new IllegalArgumentException("startOffset < 0: " + startOffset);
        }
        if (endOffset < startOffset) {
            throw new IllegalArgumentException("endOffset < startOffset: " + startOffset + ".." + endOffset);
        }
    }

    public int length() {
        return endOffset - startOffset;
    }

    public boolean isEmpty() {
        return startOffset == endOffset;
    }

    public boolean contains(int offset) {
        return startOffset <= offset && offset <= endOffset;
    }

    public boolean contains(TextRange other) {
        return other != null && startOffset <= other.startOffset && other.endOffset <= endOffset;
    }

    public boolean intersects(TextRange other) {
        return other != null && startOffset < other.endOffset && other.startOffset < endOffset;
    }

    public TextRange shift(int delta) {
        if (delta == 0) return this;
        return new TextRange(startOffset + delta, endOffset + delta);
    }

    public TextRange intersection(TextRange other) {
        if (other == null) return null;
        int start = Math.max(startOffset, other.startOffset);
        int end = Math.min(endOffset, other.endOffset);
        if (start > end) return null;
        return new TextRange(start, end);
    }

    public static TextRange of(int start, int end) {
        return new TextRange(start, end);
    }
}
