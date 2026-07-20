package com.eyecode.learning.markdown.layout;

public record ComponentLayout(
        int spaceAbove,
        int spaceBelow,
        int leftIndent,
        int rightIndent,
        float lineSpacing,
        int paddingTop,
        int paddingBottom
) {

    public static final ComponentLayout NONE = new ComponentLayout(0, 0, 0, 0, 0f, 0, 0);
}
