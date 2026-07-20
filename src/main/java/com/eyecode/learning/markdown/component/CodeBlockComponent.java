package com.eyecode.learning.markdown.component;

public record CodeBlockComponent(String language, String code) implements MarkdownComponent {
}
