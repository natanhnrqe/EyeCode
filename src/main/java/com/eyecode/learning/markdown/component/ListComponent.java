package com.eyecode.learning.markdown.component;

public record ListComponent(java.util.List<String> items, boolean ordered) implements MarkdownComponent {
}
