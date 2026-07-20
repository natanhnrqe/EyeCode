package com.eyecode.learning.markdown.component;

import com.eyecode.learning.markdown.Segment;

import java.util.List;

public record ParagraphComponent(List<Segment> segments) implements MarkdownComponent {
}
