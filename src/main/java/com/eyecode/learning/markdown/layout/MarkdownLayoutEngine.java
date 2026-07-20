package com.eyecode.learning.markdown.layout;

import com.eyecode.learning.markdown.component.*;

public final class MarkdownLayoutEngine {

    private static final int CONTENT_LEFT_MARGIN = 16;
    private static final int CONTENT_RIGHT_MARGIN = 24;

    private static final int H1_SPACE_BELOW = 14;
    private static final int H2_SPACE_BELOW = 12;
    private static final int H3_SPACE_BELOW = 10;
    private static final int BODY_SPACE_BELOW = 14;
    private static final float BODY_LINE_SPACING = 0.30f;
    private static final int BULLET_SPACE_BELOW = 14;
    private static final int BULLET_LEFT_INDENT = 16;
    private static final float BULLET_LINE_SPACING = 0.25f;
    private static final int CODE_PADDING_TOP = 10;
    private static final int CODE_PADDING_BOTTOM = 14;
    private static final int DIVIDER_SPACE_BELOW = 14;

    public ComponentLayout layout(MarkdownComponent component) {
        return switch (component) {
            case HeadingComponent h -> headingLayout(h);
            case ParagraphComponent p -> paragraphLayout(p);
            case ListComponent l -> listLayout(l);
            case CodeBlockComponent c -> codeLayout(c);
            case DividerComponent d -> dividerLayout(d);
        };
    }

    private ComponentLayout headingLayout(HeadingComponent heading) {
        int spaceBelow = switch (heading.level()) {
            case 1 -> H1_SPACE_BELOW;
            case 2 -> H2_SPACE_BELOW;
            default -> H3_SPACE_BELOW;
        };
        return new ComponentLayout(0, spaceBelow,
                CONTENT_LEFT_MARGIN, CONTENT_RIGHT_MARGIN, 0f, 0, 0);
    }

    private ComponentLayout paragraphLayout(ParagraphComponent paragraph) {
        return new ComponentLayout(0, BODY_SPACE_BELOW,
                CONTENT_LEFT_MARGIN, CONTENT_RIGHT_MARGIN, BODY_LINE_SPACING, 0, 0);
    }

    private ComponentLayout listLayout(ListComponent list) {
        return new ComponentLayout(0, BULLET_SPACE_BELOW,
                CONTENT_LEFT_MARGIN + BULLET_LEFT_INDENT, CONTENT_RIGHT_MARGIN,
                BULLET_LINE_SPACING, 0, 0);
    }

    private ComponentLayout codeLayout(CodeBlockComponent codeBlock) {
        return new ComponentLayout(0, 0,
                CONTENT_LEFT_MARGIN, CONTENT_RIGHT_MARGIN,
                0f, CODE_PADDING_TOP, CODE_PADDING_BOTTOM);
    }

    private ComponentLayout dividerLayout(DividerComponent divider) {
        return new ComponentLayout(0, DIVIDER_SPACE_BELOW,
                CONTENT_LEFT_MARGIN, CONTENT_RIGHT_MARGIN, 0f, 0, 0);
    }

    public ComponentLayout arrowLayout() {
        return new ComponentLayout(10, 10,
                CONTENT_LEFT_MARGIN, CONTENT_RIGHT_MARGIN, 0f, 0, 0);
    }

    public ComponentLayout linkLayout() {
        return new ComponentLayout(12, 6, 0, 0, 0f, 0, 0);
    }
}
