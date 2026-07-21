package com.eyecode.learning.markdown.layout;

import static com.eyecode.learning.markdown.theme.MarkdownDesignTokens.*;

import com.eyecode.learning.markdown.component.*;

public final class MarkdownLayoutEngine {

    private static final int DEFAULT_VIEWPORT_WIDTH = 800;

    public ComponentLayout layout(MarkdownComponent component) {
        return layout(component, DEFAULT_VIEWPORT_WIDTH);
    }

    public ComponentLayout layout(MarkdownComponent component, int viewportWidth) {
        int margin = contentMargin(viewportWidth);
        int rightMargin = margin + SCROLLBAR_SAFE_ZONE;
        return switch (component) {
            case HeadingComponent h -> headingLayout(h, margin, rightMargin);
            case ParagraphComponent p -> paragraphLayout(p, margin, rightMargin);
            case ListComponent l -> listLayout(l, margin, rightMargin);
            case CodeBlockComponent c -> codeLayout(c, margin, rightMargin);
            case DividerComponent d -> dividerLayout(d, margin, rightMargin);
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

    private ComponentLayout headingLayout(HeadingComponent heading, int margin, int rightMargin) {
        int spaceAbove = switch (heading.level()) {
            case 1 -> 0;
            default -> HEADING_MARGIN_TOP;
        };
        int spaceBelow = switch (heading.level()) {
            case 1 -> H1_SPACE_BELOW;
            case 2 -> H2_SPACE_BELOW;
            default -> H3_SPACE_BELOW;
        };
        return new ComponentLayout(spaceAbove, spaceBelow, margin, rightMargin, 0f, 0, 0);
    }

    private ComponentLayout paragraphLayout(ParagraphComponent paragraph, int margin, int rightMargin) {
        return new ComponentLayout(0, BODY_SPACE_BELOW,
                margin, rightMargin, BODY_LINE_SPACING, 0, 0);
    }

    private ComponentLayout listLayout(ListComponent list, int margin, int rightMargin) {
        return new ComponentLayout(0, BULLET_SPACE_BELOW,
                margin + BULLET_LEFT_INDENT, rightMargin,
                BULLET_LINE_SPACING, 0, 0);
    }

    private ComponentLayout codeLayout(CodeBlockComponent codeBlock, int margin, int rightMargin) {
        return new ComponentLayout(0, 0,
                margin, rightMargin, CODE_LINE_SPACING, CODE_PADDING_TOP, CODE_PADDING_BOTTOM);
    }

    private ComponentLayout dividerLayout(DividerComponent divider, int margin, int rightMargin) {
        return new ComponentLayout(DIVIDER_SPACE_ABOVE, DIVIDER_SPACE_BELOW,
                margin, rightMargin, 0f, 0, 0);
    }

    public ComponentLayout arrowLayout() {
        return arrowLayout(DEFAULT_VIEWPORT_WIDTH);
    }

    public ComponentLayout arrowLayout(int viewportWidth) {
        int margin = contentMargin(viewportWidth);
        int rightMargin = margin + SCROLLBAR_SAFE_ZONE;
        return new ComponentLayout(8, 8, margin, rightMargin, 0f, 0, 0);
    }

    public ComponentLayout linkLayout() {
        return new ComponentLayout(8, 4, 0, 0, 0f, 0, 0);
    }
}
