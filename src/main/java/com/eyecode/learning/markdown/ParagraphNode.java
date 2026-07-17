package com.eyecode.learning.markdown;

import java.util.List;

public record ParagraphNode(List<Segment> segments) implements MarkdownNode {
}
