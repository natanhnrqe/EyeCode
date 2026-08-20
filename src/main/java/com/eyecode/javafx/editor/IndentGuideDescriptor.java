package com.eyecode.javafx.editor;

public record IndentGuideDescriptor(int column, int startParagraph, int endParagraph) {

    public IndentGuideDescriptor {
        if (column <= 0) {
            throw new IllegalArgumentException("column must be positive");
        }
        if (startParagraph < 0) {
            throw new IllegalArgumentException("startParagraph must be non-negative");
        }
        if (endParagraph < startParagraph) {
            throw new IllegalArgumentException("endParagraph must be >= startParagraph");
        }
    }

    public boolean intersects(int firstParagraph, int lastParagraph) {
        return endParagraph >= firstParagraph && startParagraph <= lastParagraph;
    }
}
