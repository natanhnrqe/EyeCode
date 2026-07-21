package com.eyecode.learning.markdown;

import com.eyecode.learning.markdown.theme.MarkdownColorPalette;
import com.eyecode.learning.markdown.theme.MarkdownDesignTokens;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.awt.Font;

public final class MarkdownTheme {

    private MarkdownTheme() {
    }

    private static final Color CODE_BLOCK_KEYWORD = new Color(204, 120, 50);
    private static final Color CODE_BLOCK_TYPE = new Color(106, 135, 89);
    private static final Color CODE_BLOCK_NUMBER = new Color(104, 151, 187);

    // ──────────────────────────────────────────────
    // HeadingStyles
    // ──────────────────────────────────────────────

    public static SimpleAttributeSet h1() {
        return create(TypographyManager.monoBold(MarkdownDesignTokens.HEADING1_SIZE),
                ColorManager.TEXT_PRIMARY, null, 0, MarkdownDesignTokens.H1_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet h2() {
        return create(TypographyManager.monoBold(MarkdownDesignTokens.HEADING2_SIZE),
                ColorManager.TEXT_PRIMARY, null, 0, MarkdownDesignTokens.H2_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet h3() {
        return create(TypographyManager.monoBold(MarkdownDesignTokens.HEADING3_SIZE),
                ColorManager.TEXT_PRIMARY, null, MarkdownDesignTokens.H3_SPACE_ABOVE, MarkdownDesignTokens.H3_SPACE_BELOW, 0, 0, 0.0f);
    }

    // ──────────────────────────────────────────────
    // ParagraphStyles
    // ──────────────────────────────────────────────

    public static SimpleAttributeSet paragraph() {
        return create(TypographyManager.monoRegular(MarkdownDesignTokens.BODY_SIZE),
                ColorManager.TEXT_SECONDARY, null, 0, MarkdownDesignTokens.BODY_SPACE_BELOW, 0, 0, MarkdownDesignTokens.BODY_LINE_SPACING);
    }

    public static SimpleAttributeSet paragraphMuted() {
        return create(TypographyManager.monoRegular(MarkdownDesignTokens.BODY_SIZE),
                ColorManager.TEXT_TERTIARY, null, 0, MarkdownDesignTokens.BODY_SPACE_BELOW, 0, 0, MarkdownDesignTokens.BODY_LINE_SPACING);
    }

    // ──────────────────────────────────────────────
    // ListStyles
    // ──────────────────────────────────────────────

    public static SimpleAttributeSet bullet() {
        return create(TypographyManager.monoRegular(MarkdownDesignTokens.BODY_SIZE),
                ColorManager.TEXT_PRIMARY, null, 0, MarkdownDesignTokens.BULLET_SPACE_BELOW,
                MarkdownDesignTokens.BULLET_LEFT_INDENT, 0, MarkdownDesignTokens.BULLET_LINE_SPACING);
    }

    public static SimpleAttributeSet flowArrow() {
        return create(TypographyManager.monoBold(MarkdownDesignTokens.ARROW_SIZE),
                ColorManager.TEXT_TERTIARY, null,
                MarkdownDesignTokens.ARROW_SPACE_ABOVE, MarkdownDesignTokens.ARROW_SPACE_BELOW, 0, 0, 0.0f);
    }

    // ──────────────────────────────────────────────
    // DividerStyles
    // ──────────────────────────────────────────────

    public static SimpleAttributeSet divider() {
        return create(TypographyManager.monoRegular(MarkdownDesignTokens.BODY_SIZE),
                ColorManager.BORDER_CARD, null,
                MarkdownDesignTokens.DIVIDER_SPACE_ABOVE, MarkdownDesignTokens.DIVIDER_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static String dividerText() {
        return MarkdownDesignTokens.DIVIDER_TEXT;
    }

    // ──────────────────────────────────────────────
    // CodeStyles
    // ──────────────────────────────────────────────

    public static SimpleAttributeSet codeBlock() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        Font font = TypographyManager.monoRegular(MarkdownDesignTokens.CODE_SIZE);
        StyleConstants.setFontFamily(attrs, font.getFamily());
        StyleConstants.setFontSize(attrs, font.getSize());
        StyleConstants.setForeground(attrs, ColorManager.EDITOR_FOREGROUND);
        return attrs;
    }

    public static SimpleAttributeSet codeParagraph(boolean firstLine, boolean lastLine) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBackground(attrs, ColorManager.EDITOR_BG);
        StyleConstants.setLeftIndent(attrs, MarkdownDesignTokens.CODE_PADDING_LEFT);
        StyleConstants.setRightIndent(attrs, MarkdownDesignTokens.CODE_PADDING_RIGHT);
        StyleConstants.setSpaceAbove(attrs, firstLine ? MarkdownDesignTokens.CODE_PADDING_TOP : 0);
        StyleConstants.setSpaceBelow(attrs, lastLine ? MarkdownDesignTokens.CODE_PADDING_BOTTOM : 0);
        return attrs;
    }

    public static SimpleAttributeSet codeInline() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        Font font = TypographyManager.monoRegular(MarkdownDesignTokens.INLINE_CODE_SIZE);
        StyleConstants.setFontFamily(attrs, font.getFamily());
        StyleConstants.setFontSize(attrs, font.getSize());
        StyleConstants.setForeground(attrs, ColorManager.TEXT_SECONDARY);
        StyleConstants.setBackground(attrs, ColorManager.SURFACE_BG);
        return attrs;
    }

    public static SimpleAttributeSet codeKeyword() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, CODE_BLOCK_KEYWORD);
        return attrs;
    }

    public static SimpleAttributeSet codeType() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, CODE_BLOCK_TYPE);
        return attrs;
    }

    public static SimpleAttributeSet codeString() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, ColorManager.SYNTAX_STRING);
        return attrs;
    }

    public static SimpleAttributeSet codeComment() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, ColorManager.SYNTAX_COMMENT);
        return attrs;
    }

    public static SimpleAttributeSet codeAnnotation() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, ColorManager.SYNTAX_ANNOTATION);
        return attrs;
    }

    public static SimpleAttributeSet codeNumber() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setForeground(attrs, CODE_BLOCK_NUMBER);
        return attrs;
    }

    // ──────────────────────────────────────────────
    // CalloutStyles
    // ──────────────────────────────────────────────

    public static SimpleAttributeSet calloutContainer(String type) {
        Color bg = switch (type.toLowerCase()) {
            case "tip" -> MarkdownColorPalette.CALLOUT_TIP_BG;
            case "warning" -> MarkdownColorPalette.CALLOUT_WARN_BG;
            default -> MarkdownColorPalette.CALLOUT_INFO_BG;
        };
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBackground(attrs, bg);
        StyleConstants.setLeftIndent(attrs, MarkdownDesignTokens.CALLOUT_LEFT_INDENT);
        return attrs;
    }

    public static SimpleAttributeSet calloutTitleParagraph() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setSpaceAbove(attrs, MarkdownDesignTokens.CALLOUT_TITLE_SPACE_ABOVE);
        StyleConstants.setSpaceBelow(attrs, MarkdownDesignTokens.CALLOUT_TITLE_SPACE_BELOW);
        return attrs;
    }

    public static SimpleAttributeSet calloutBodyParagraph() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setSpaceAbove(attrs, MarkdownDesignTokens.CALLOUT_BODY_SPACE_ABOVE);
        StyleConstants.setSpaceBelow(attrs, MarkdownDesignTokens.CALLOUT_BODY_SPACE_BELOW);
        return attrs;
    }

    public static SimpleAttributeSet calloutTitle(String type) {
        Color fg = switch (type.toLowerCase()) {
            case "tip" -> ColorManager.SUCCESS_GREEN;
            case "warning" -> ColorManager.ERROR_RED;
            default -> ColorManager.ACCENT_BLUE_LIGHT;
        };
        return create(TypographyManager.monoBold(MarkdownDesignTokens.BODY_SIZE), fg, null, 0, 0, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet calloutBody() {
        return create(TypographyManager.monoRegular(MarkdownDesignTokens.BODY_SIZE),
                ColorManager.TEXT_SECONDARY, null, 0, 0, 0, 0, 0.0f);
    }

    // ──────────────────────────────────────────────
    // InlineStyles
    // ──────────────────────────────────────────────

    public static SimpleAttributeSet body() {
        return paragraph();
    }

    public static SimpleAttributeSet bold() {
        return create(TypographyManager.monoBold(MarkdownDesignTokens.BODY_SIZE),
                ColorManager.TEXT_SECONDARY, null, 0, 0, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet italic() {
        return create(TypographyManager.monoRegular(MarkdownDesignTokens.BODY_SIZE),
                ColorManager.TEXT_SECONDARY, null, 0, 0, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet link() {
        SimpleAttributeSet attrs = create(TypographyManager.monoRegular(MarkdownDesignTokens.BODY_SIZE),
                ColorManager.ACCENT_BLUE_LIGHT, null,
                MarkdownDesignTokens.LINK_SPACE_ABOVE, MarkdownDesignTokens.LINK_SPACE_BELOW, 0, 0, 0.0f);
        StyleConstants.setUnderline(attrs, true);
        return attrs;
    }

    public static SimpleAttributeSet arrow() {
        return flowArrow();
    }

    // ──────────────────────────────────────────────
    // Legacy
    // ──────────────────────────────────────────────

    public static SimpleAttributeSet callout() {
        return create(TypographyManager.monoRegular(MarkdownDesignTokens.BODY_SIZE),
                ColorManager.TEXT_SECONDARY, null, 0, MarkdownDesignTokens.CALLOUT_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet codeBlockParagraph(boolean firstLine, boolean lastLine) {
        return codeParagraph(firstLine, lastLine);
    }

    static SimpleAttributeSet create(Font font, Color fg, Color bg,
                                     int spaceAbove, int spaceBelow,
                                     int leftIndent, int rightIndent,
                                     float lineSpacing) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, font.getFamily());
        StyleConstants.setFontSize(attrs, font.getSize());
        StyleConstants.setBold(attrs, font.isBold());
        StyleConstants.setItalic(attrs, font.isItalic());
        StyleConstants.setForeground(attrs, fg);
        if (bg != null) {
            StyleConstants.setBackground(attrs, bg);
        }
        StyleConstants.setSpaceAbove(attrs, spaceAbove);
        StyleConstants.setSpaceBelow(attrs, spaceBelow);
        StyleConstants.setLeftIndent(attrs, leftIndent);
        StyleConstants.setRightIndent(attrs, rightIndent);
        StyleConstants.setFirstLineIndent(attrs, 0);
        StyleConstants.setLineSpacing(attrs, lineSpacing);
        return attrs;
    }
}
