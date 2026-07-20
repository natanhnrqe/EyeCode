package com.eyecode.learning.markdown;

public sealed interface MarkdownNode
        permits HeadingNode, ParagraphNode, BulletNode,
                CodeBlockNode, DividerNode {
}
