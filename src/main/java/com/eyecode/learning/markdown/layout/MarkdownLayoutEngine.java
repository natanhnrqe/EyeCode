package com.eyecode.learning.markdown.layout;

import com.eyecode.learning.markdown.component.*;

public final class MarkdownLayoutEngine {

    private static final int DEFAULT_VIEWPORT_WIDTH = 800;

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
        return layout(component, DEFAULT_VIEWPORT_WIDTH);
    }

    public ComponentLayout layout(MarkdownComponent component, int viewportWidth) {
        int margin = contentMargin(viewportWidth);
        return switch (component) {
            case HeadingComponent h -> headingLayout(h, margin);
            case ParagraphComponent p -> paragraphLayout(p, margin);
            case ListComponent l -> listLayout(l, margin);
            case CodeBlockComponent c -> codeLayout(c, margin);
            case DividerComponent d -> dividerLayout(d, margin);
        };
    }

    private static int contentMargin(int viewportWidth) {
        if (viewportWidth > 1000) {
            return 64;
        }
        if (viewportWidth > 800) {
            return 40;
        }
        if (viewportWidth > 600) {
            return 24;
        }
        return 16;
    }

    private ComponentLayout headingLayout(HeadingComponent heading, int margin) {
        int spaceBelow = switch (heading.level()) {
            case 1 -> H1_SPACE_BELOW;
            case 2 -> H2_SPACE_BELOW;
            default -> H3_SPACE_BELOW;
        };
        return new ComponentLayout(0, spaceBelow, margin, margin, 0f, 0, 0);
    }

    private ComponentLayout paragraphLayout(ParagraphComponent paragraph, int margin) {
        return new ComponentLayout(0, BODY_SPACE_BELOW,
                margin, margin, BODY_LINE_SPACING, 0, 0);
    }

    private ComponentLayout listLayout(ListComponent list, int margin) {
        return new ComponentLayout(0, BULLET_SPACE_BELOW,
                margin + BULLET_LEFT_INDENT, margin,
                BULLET_LINE_SPACING, 0, 0);
    }

    private ComponentLayout codeLayout(CodeBlockComponent codeBlock, int margin) {
        return new ComponentLayout(0, 0,
                margin, margin, 0f, CODE_PADDING_TOP, CODE_PADDING_BOTTOM);
    }

    private ComponentLayout dividerLayout(DividerComponent divider, int margin) {
        return new ComponentLayout(0, DIVIDER_SPACE_BELOW,
                margin, margin, 0f, 0, 0);
    }

    public ComponentLayout arrowLayout() {
        return arrowLayout(DEFAULT_VIEWPORT_WIDTH);
    }

    public ComponentLayout arrowLayout(int viewportWidth) {
        int margin = contentMargin(viewportWidth);
        return new ComponentLayout(10, 10, margin, margin, 0f, 0, 0);
    }

    public ComponentLayout linkLayout() {
        return new ComponentLayout(12, 6, 0, 0, 0f, 0, 0);
    }
}
