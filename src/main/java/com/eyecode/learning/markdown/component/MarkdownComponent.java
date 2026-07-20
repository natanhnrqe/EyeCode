package com.eyecode.learning.markdown.component;

public sealed interface MarkdownComponent
        permits HeadingComponent, ParagraphComponent,
                DividerComponent, CodeBlockComponent, ListComponent {
}
