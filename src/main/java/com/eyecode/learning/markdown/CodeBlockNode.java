package com.eyecode.learning.markdown;

public record CodeBlockNode(String language, String code) implements MarkdownNode {
}
