package com.eyecode.learning.markdown;

public record BulletNode(String text, boolean checked) implements MarkdownNode {
}
