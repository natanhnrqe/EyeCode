package com.eyecode.learning.document;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

public final class LearningDocumentStyle {

    private static final int POPUP_CURSOR_OFFSET = 12;
    private static final int POPUP_WIDTH = 400;
    private static final int POPUP_HEIGHT = 480;
    private static final int CARD_ARC = 10;
    private static final int CARD_ORIGIN = 0;
    private static final int CARD_BORDER_INSET = 1;

    private static final int PAGE_TITLE_FONT_SIZE = 17;
    private static final int META_FONT_SIZE = 11;
    private static final int SECTION_FONT_SIZE = 12;
    private static final int BODY_FONT_SIZE = 13;
    private static final int BULLET_FONT_SIZE = 12;
    private static final int LINK_FONT_SIZE = 12;
    private static final int ARROW_FONT_SIZE = 14;
    private static final int BUTTON_FONT_SIZE = 12;

    private static final int HEADER_TOP = 16;
    private static final int HEADER_LEFT = 16;
    private static final int HEADER_BOTTOM = 6;
    private static final int HEADER_RIGHT = 16;
    private static final int HEADER_TEXT_GAP = 2;
    private static final int TITLE_ICON_GAP = 8;

    private static final int DOCUMENT_TOP = 12;
    private static final int DOCUMENT_LEFT = 16;
    private static final int DOCUMENT_BOTTOM = 12;
    private static final int DOCUMENT_RIGHT = 16;
    private static final int DOCUMENT_SCROLL_UNIT = 16;

    private static final int FOOTER_TOP = 4;
    private static final int FOOTER_LEFT = 0;
    private static final int FOOTER_BOTTOM = 8;
    private static final int FOOTER_RIGHT = 0;
    private static final int FOOTER_HORIZONTAL_GAP = 0;
    private static final int FOOTER_VERTICAL_GAP = 8;

    private static final int BUTTON_TOP = 6;
    private static final int BUTTON_LEFT = 0;
    private static final int BUTTON_BOTTOM = 6;
    private static final int BUTTON_RIGHT = 0;

    private static final int TITLE_LEFT_INDENT = 0;
    private static final int TITLE_RIGHT_INDENT = 0;
    private static final int BODY_LEFT_INDENT = 0;
    private static final int BODY_RIGHT_INDENT = 0;
    private static final int BULLET_LEFT_INDENT = 12;
    private static final int BULLET_RIGHT_INDENT = 0;
    private static final int CODE_LEFT_INDENT = 12;
    private static final int CODE_RIGHT_INDENT = 12;

    private static final float BODY_LINE_SPACING = 0.18f;
    private static final float BULLET_LINE_SPACING = 0.14f;
    private static final float CODE_LINE_SPACING = 0.10f;

    private static final String DIVIDER_TEXT = "────────────────────────────────";

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private static final SimpleAttributeSet PAGE_TITLE;
    private static final SimpleAttributeSet META_PRIMARY;
    private static final SimpleAttributeSet META_SECONDARY;
    private static final SimpleAttributeSet SECTION_TITLE;
    private static final SimpleAttributeSet BODY;
    private static final SimpleAttributeSet CODE;
    private static final SimpleAttributeSet BULLET;
    private static final SimpleAttributeSet LINK;
    private static final SimpleAttributeSet ARROW;
    private static final SimpleAttributeSet DIVIDER;

    static {
        PAGE_TITLE = create(pageTitleFont(), ColorManager.TEXT_PRIMARY, null, 0, 4, TITLE_LEFT_INDENT, TITLE_RIGHT_INDENT, 0.0f);
        META_PRIMARY = create(metaFont(), ColorManager.SUCCESS_GREEN, null, 0, 0, 0, 0, 0.0f);
        META_SECONDARY = create(metaFont(), ColorManager.TEXT_TERTIARY, null, 0, 0, 0, 0, 0.0f);
        SECTION_TITLE = create(sectionTitleFont(), ColorManager.TEXT_PRIMARY, null, 14, 6, TITLE_LEFT_INDENT, TITLE_RIGHT_INDENT, 0.0f);
        BODY = create(bodyFont(), ColorManager.TEXT_SECONDARY, null, 0, 6, BODY_LEFT_INDENT, BODY_RIGHT_INDENT, BODY_LINE_SPACING);
        CODE = create(codeFont(), ColorManager.EDITOR_FOREGROUND, ColorManager.PANEL_BG, 10, 10, CODE_LEFT_INDENT, CODE_RIGHT_INDENT, CODE_LINE_SPACING);
        BULLET = create(bulletFont(), ColorManager.TEXT_PRIMARY, null, 0, 4, BULLET_LEFT_INDENT, BULLET_RIGHT_INDENT, BULLET_LINE_SPACING);
        LINK = create(linkFont(), ColorManager.ACCENT_BLUE_LIGHT, null, 10, 4, TITLE_LEFT_INDENT, TITLE_RIGHT_INDENT, 0.0f);
        ARROW = create(arrowFont(), ColorManager.TEXT_TERTIARY, null, 0, 0, 0, 0, 0.0f);
        DIVIDER = create(bodyFont(), ColorManager.BORDER_DIVIDER, null, 10, 10, 0, 0, 0.0f);
    }

    private LearningDocumentStyle() {
    }

    public static SimpleAttributeSet title() {
        return copy(PAGE_TITLE);
    }

    public static SimpleAttributeSet subtitle() {
        return copy(META_SECONDARY);
    }

    public static SimpleAttributeSet metaPrimary() {
        return copy(META_PRIMARY);
    }

    public static SimpleAttributeSet metaSecondary() {
        return copy(META_SECONDARY);
    }

    public static SimpleAttributeSet sectionTitle() {
        return copy(SECTION_TITLE);
    }

    public static SimpleAttributeSet body() {
        return copy(BODY);
    }

    public static SimpleAttributeSet code() {
        return copy(CODE);
    }

    public static SimpleAttributeSet bullet() {
        return copy(BULLET);
    }

    public static SimpleAttributeSet link() {
        return copy(LINK);
    }

    public static SimpleAttributeSet arrow() {
        return copy(ARROW);
    }

    public static SimpleAttributeSet divider() {
        return copy(DIVIDER);
    }

    public static SimpleAttributeSet dividerLeft() {
        return copy(DIVIDER);
    }

    public static SimpleAttributeSet dividerRight() {
        return copy(DIVIDER);
    }

    public static String dividerText() {
        return DIVIDER_TEXT;
    }

    public static Dimension popupSize() {
        return new Dimension(POPUP_WIDTH, POPUP_HEIGHT);
    }

    public static int popupCursorOffset() {
        return POPUP_CURSOR_OFFSET;
    }

    public static int cardArc() {
        return CARD_ARC;
    }

    public static int cardOrigin() {
        return CARD_ORIGIN;
    }

    public static int cardBorderInset() {
        return CARD_BORDER_INSET;
    }

    public static Color cardBackground() {
        return ColorManager.CARD_BG;
    }

    public static Color cardBorderColor() {
        return ColorManager.BORDER;
    }

    public static Color transparent() {
        return TRANSPARENT;
    }

    public static Border headerBorder() {
        return BorderFactory.createEmptyBorder(HEADER_TOP, HEADER_LEFT, HEADER_BOTTOM, HEADER_RIGHT);
    }

    public static int headerTextGap() {
        return HEADER_TEXT_GAP;
    }

    public static Color dividerColor() {
        return ColorManager.BORDER_DIVIDER;
    }

    public static Border documentBorder() {
        return BorderFactory.createEmptyBorder(DOCUMENT_TOP, DOCUMENT_LEFT, DOCUMENT_BOTTOM, DOCUMENT_RIGHT);
    }

    public static int documentScrollUnit() {
        return DOCUMENT_SCROLL_UNIT;
    }

    public static Border footerBorder() {
        return BorderFactory.createEmptyBorder(FOOTER_TOP, FOOTER_LEFT, FOOTER_BOTTOM, FOOTER_RIGHT);
    }

    public static int footerHorizontalGap() {
        return FOOTER_HORIZONTAL_GAP;
    }

    public static int footerVerticalGap() {
        return FOOTER_VERTICAL_GAP;
    }

    public static Font pageTitleFont() {
        return TypographyManager.monoBold(PAGE_TITLE_FONT_SIZE);
    }

    public static Font titleFont() {
        return pageTitleFont();
    }

    public static Font metaFont() {
        return TypographyManager.monoRegular(META_FONT_SIZE);
    }

    public static Font subtitleFont() {
        return metaFont();
    }

    public static Font sectionTitleFont() {
        return TypographyManager.monoBold(SECTION_FONT_SIZE);
    }

    public static Font bodyFont() {
        return TypographyManager.monoRegular(BODY_FONT_SIZE);
    }

    public static Font codeFont() {
        return TypographyManager.monoRegular(BODY_FONT_SIZE);
    }

    public static Font bulletFont() {
        return TypographyManager.monoRegular(BULLET_FONT_SIZE);
    }

    public static Font linkFont() {
        return TypographyManager.monoRegular(LINK_FONT_SIZE);
    }

    public static Font arrowFont() {
        return TypographyManager.monoBold(ARROW_FONT_SIZE);
    }

    public static Font buttonFont() {
        return TypographyManager.monoRegular(BUTTON_FONT_SIZE);
    }

    public static Color titleColor() {
        return ColorManager.TEXT_PRIMARY;
    }

    public static Color subtitleColor() {
        return ColorManager.TEXT_TERTIARY;
    }

    public static Color metaPrimaryColor() {
        return ColorManager.SUCCESS_GREEN;
    }

    public static Color metaSecondaryColor() {
        return ColorManager.TEXT_TERTIARY;
    }

    public static Color buttonColor() {
        return ColorManager.ACCENT_BLUE_LIGHT;
    }

    public static Color bodyColor() {
        return ColorManager.TEXT_SECONDARY;
    }

    public static Color codeBackgroundColor() {
        return ColorManager.PANEL_BG;
    }

    public static Insets zeroInsets() {
        return new Insets(0, 0, 0, 0);
    }

    public static int titleIconTextGap() {
        return TITLE_ICON_GAP;
    }

    public static Border emptyBorder() {
        return BorderFactory.createEmptyBorder(0, 0, 0, 0);
    }

    public static Border buttonBorder() {
        return BorderFactory.createEmptyBorder(BUTTON_TOP, BUTTON_LEFT, BUTTON_BOTTOM, BUTTON_RIGHT);
    }

    private static SimpleAttributeSet create(
            Font font,
            Color fg,
            Color bg,
            int spaceAbove,
            int spaceBelow,
            int leftIndent,
            int rightIndent,
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

    private static SimpleAttributeSet copy(SimpleAttributeSet source) {
        return new SimpleAttributeSet(source);
    }
}
