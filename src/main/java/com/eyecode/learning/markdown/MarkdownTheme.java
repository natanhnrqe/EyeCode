package com.eyecode.learning.markdown;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.awt.Font;

public final class MarkdownTheme {

    private static final int H1_SIZE = 18;
    private static final int H2_SIZE = 13;
    private static final int BODY_SIZE = 12;
    private static final int CODE_SIZE = 12;
    private static final int BULLET_SIZE = 12;
    private static final int LINK_SIZE = 12;
    private static final int ARROW_SIZE = 14;
    private static final int CALLOUT_SIZE = 12;

    private static final int H1_SPACE_BELOW = 6;
    private static final int H2_SPACE_ABOVE = 18;
    private static final int H2_SPACE_BELOW = 10;
    private static final int BODY_SPACE_BELOW = 9;
    private static final int BULLET_SPACE_BELOW = 7;
    private static final int LINK_SPACE_ABOVE = 12;
    private static final int LINK_SPACE_BELOW = 6;
    private static final int ARROW_SPACE_ABOVE = 0;
    private static final int ARROW_SPACE_BELOW = 0;
    private static final int CALLOUT_SPACE_BELOW = 9;
    private static final int CODE_LEFT_INDENT = 24;
    private static final int CODE_RIGHT_INDENT = 0;
    private static final int BULLET_LEFT_INDENT = 14;
    private static final int DIVIDER_SPACE_ABOVE = 18;
    private static final int DIVIDER_SPACE_BELOW = 18;

    private static final float BODY_LINE_SPACING = 0.18f;
    private static final float CODE_LINE_SPACING = 0.08f;
    private static final float BULLET_LINE_SPACING = 0.20f;

    private static final String DIVIDER_TEXT = "\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500\u2500";
    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);
    private static final Color CALLOUT_INFO_BG = new Color(0x1A, 0x73, 0xE8, 0x0F);
    private static final Color CALLOUT_WARN_BG = new Color(0xFF, 0xA0, 0x00, 0x0F);
    private static final Color CALLOUT_TIP_BG = new Color(0x00, 0xA8, 0x6B, 0x0F);

    private MarkdownTheme() {
    }

    public static SimpleAttributeSet h1() {
        return create(TypographyManager.monoBold(H1_SIZE),
                ColorManager.TEXT_PRIMARY, null, 0, H1_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet h2() {
        return create(TypographyManager.monoBold(H2_SIZE),
                ColorManager.TEXT_PRIMARY, null, H2_SPACE_ABOVE, H2_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet h2Colored(Color color) {
        return create(TypographyManager.monoBold(H2_SIZE),
                color, null, H2_SPACE_ABOVE, H2_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet body() {
        return create(TypographyManager.monoRegular(BODY_SIZE),
                ColorManager.TEXT_SECONDARY, null, 0, BODY_SPACE_BELOW, 0, 0, BODY_LINE_SPACING);
    }

    public static SimpleAttributeSet bold() {
        return create(TypographyManager.monoBold(BODY_SIZE),
                ColorManager.TEXT_SECONDARY, null, 0, 0, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet italic() {
        return create(TypographyManager.monoRegular(BODY_SIZE),
                ColorManager.TEXT_SECONDARY, null, 0, 0, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet codeInline() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, TypographyManager.monoRegular(CODE_SIZE).getFamily());
        StyleConstants.setFontSize(attrs, CODE_SIZE);
        StyleConstants.setForeground(attrs, ColorManager.SYNTAX_KEYWORD);
        StyleConstants.setBackground(attrs, ColorManager.PANEL_BG);
        return attrs;
    }

    public static SimpleAttributeSet codeBlock() {
        return create(TypographyManager.monoRegular(CODE_SIZE),
                ColorManager.EDITOR_FOREGROUND, null, 0, 0,
                CODE_LEFT_INDENT, CODE_RIGHT_INDENT, CODE_LINE_SPACING);
    }

    public static SimpleAttributeSet codeParagraph() {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setBackground(attrs, ColorManager.EDITOR_BG);
        return attrs;
    }

    public static SimpleAttributeSet bullet() {
        return create(TypographyManager.monoRegular(BULLET_SIZE),
                ColorManager.TEXT_PRIMARY, null, 0, BULLET_SPACE_BELOW,
                BULLET_LEFT_INDENT, 0, BULLET_LINE_SPACING);
    }

    public static SimpleAttributeSet link() {
        return create(TypographyManager.monoRegular(LINK_SIZE),
                ColorManager.ACCENT_BLUE_LIGHT, null,
                LINK_SPACE_ABOVE, LINK_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet arrow() {
        return create(TypographyManager.monoBold(ARROW_SIZE),
                ColorManager.TEXT_TERTIARY, null,
                ARROW_SPACE_ABOVE, ARROW_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet divider() {
        return create(TypographyManager.monoRegular(BODY_SIZE),
                ColorManager.BORDER_CARD, null,
                DIVIDER_SPACE_ABOVE, DIVIDER_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static SimpleAttributeSet callout() {
        return create(TypographyManager.monoRegular(CALLOUT_SIZE),
                ColorManager.TEXT_SECONDARY, null, 0, CALLOUT_SPACE_BELOW, 0, 0, 0.0f);
    }

    public static Color calloutBackground(String type) {
        if (type == null) {
            return TRANSPARENT;
        }
        return switch (type.toLowerCase()) {
            case "info" -> CALLOUT_INFO_BG;
            case "warning" -> CALLOUT_WARN_BG;
            case "tip" -> CALLOUT_TIP_BG;
            default -> TRANSPARENT;
        };
    }

    public static Color transparent() {
        return TRANSPARENT;
    }

    public static Color sectionColor(String emojiPrefix) {
        if (emojiPrefix == null || emojiPrefix.isEmpty()) {
            return ColorManager.TEXT_PRIMARY;
        }
        return switch (emojiPrefix) {
            case "\uD83D\uDCA1" -> ColorManager.ACCENT_BLUE_LIGHT;
            case "\uD83C\uDFE0" -> ColorManager.SYNTAX_CLASS;
            case "\uD83C\uDF0E" -> ColorManager.SUCCESS_GREEN;
            case "\uD83D\uDCBB" -> ColorManager.SYNTAX_KEYWORD;
            case "\uD83E\uDDE0" -> ColorManager.SYNTAX_CLASS;
            case "\u26A0\uFE0F" -> ColorManager.ERROR_RED;
            case "\uD83D\uDE80" -> ColorManager.TEXT_PRIMARY;
            case "\u27A1\uFE0F" -> ColorManager.ACCENT_BLUE_LIGHT;
            case "\uD83D\uDCDA" -> ColorManager.TEXT_TERTIARY;
            default -> ColorManager.TEXT_PRIMARY;
        };
    }

    public static String dividerText() {
        return DIVIDER_TEXT;
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
