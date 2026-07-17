package com.eyecode.learning.markdown;

public record HeadingNode(int level, String text) implements MarkdownNode {
    public HeadingNode {
        if (level < 1 || level > 3) {
            throw new IllegalArgumentException("level must be 1, 2, or 3");
        }
    }
}
