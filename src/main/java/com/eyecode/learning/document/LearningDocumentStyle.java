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
    private static final int POPUP_WIDTH = 500;
    private static final int POPUP_HEIGHT = 500;
    private static final int CARD_ARC = 10;
    private static final int CARD_ORIGIN = 0;
    private static final int CARD_BORDER_INSET = 1;

    private static final int PAGE_TITLE_FONT_SIZE = 18;
    private static final int META_FONT_SIZE = 11;
    private static final int SECTION_FONT_SIZE = 13;
    private static final int BODY_FONT_SIZE = 12;
    private static final int BULLET_FONT_SIZE = 12;
    private static final int CODE_FONT_SIZE = 12;
    private static final int LINK_FONT_SIZE = 12;
    private static final int ARROW_FONT_SIZE = 14;
    private static final int BUTTON_FONT_SIZE = 12;

    private static final int HEADER_TOP = 16;
    private static final int HEADER_LEFT = 16;
    private static final int HEADER_BOTTOM = 6;
    private static final int HEADER_RIGHT = 16;
    private static final int HEADER_TEXT_GAP = 2;
    private static final int TITLE_ICON_GAP = 8;

    private static final int DOCUMENT_TOP = 10;
    private static final int DOCUMENT_LEFT = 16;
    private static final int DOCUMENT_BOTTOM = 12;
    private static final int DOCUMENT_RIGHT = 16;
    private static final int DOCUMENT_SCROLL_UNIT = 16;

    private static final int FOOTER_TOP = 4;
    private static final int FOOTER_LEFT = 0;
    private static final int FOOTER_BOTTOM = 4;
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
    private static final int BULLET_LEFT_INDENT = 14;
    private static final int BULLET_RIGHT_INDENT = 0;
    private static final int CODE_LEFT_INDENT = 24;
    private static final int CODE_RIGHT_INDENT = 0;
    private static final int CODE_BLOCK_HORIZONTAL_PADDING = 0;
    private static final int CODE_BLOCK_MINIMUM_COLUMNS = 0;

    private static final float BODY_LINE_SPACING = 0.18f;
    private static final float BULLET_LINE_SPACING = 0.20f;
    private static final float CODE_LINE_SPACING = 0.08f;

    private static final String DIVIDER_TEXT = "────────────────────────────────";

    private static final Color TRANSPARENT = new Color(0, 0, 0, 0);

    private static final SimpleAttributeSet PAGE_TITLE;
    private static final SimpleAttributeSet META_PRIMARY;
    private static final SimpleAttributeSet META_SECONDARY;
    private static final SimpleAttributeSet SECTION_TITLE;
    private static final SimpleAttributeSet EXPLANATION_TITLE;
    private static final SimpleAttributeSet ANALOGY_TITLE;
    private static final SimpleAttributeSet WORLD_TITLE;
    private static final SimpleAttributeSet CODE_TITLE;
    private static final SimpleAttributeSet FLOW_TITLE;
    private static final SimpleAttributeSet MISTAKES_TITLE;
    private static final SimpleAttributeSet NEXT_STEP_TITLE;
    private static final SimpleAttributeSet REFERENCE_TITLE;
    private static final SimpleAttributeSet BODY;
    private static final SimpleAttributeSet CODE;
    private static final SimpleAttributeSet BULLET;
    private static final SimpleAttributeSet MISTAKE;
    private static final SimpleAttributeSet LINK;
    private static final SimpleAttributeSet ARROW;
    private static final SimpleAttributeSet DIVIDER;
    private static final SimpleAttributeSet CODE_KEYWORD;
    private static final SimpleAttributeSet CODE_TYPE;
    private static final SimpleAttributeSet CODE_STRING;
    private static final SimpleAttributeSet CODE_COMMENT;
    private static final SimpleAttributeSet CODE_PARAGRAPH;

    static {
        PAGE_TITLE = create(pageTitleFont(), ColorManager.TEXT_PRIMARY, null, 0, 6, TITLE_LEFT_INDENT, TITLE_RIGHT_INDENT, 0.0f);
        META_PRIMARY = create(metaFont(), ColorManager.SUCCESS_GREEN, null, 0, 0, 0, 0, 0.0f);
        META_SECONDARY = create(metaFont(), ColorManager.TEXT_TERTIARY, null, 0, 0, 0, 0, 0.0f);
        SECTION_TITLE = create(sectionTitleFont(), ColorManager.TEXT_PRIMARY, null, 18, 10, TITLE_LEFT_INDENT, TITLE_RIGHT_INDENT, 0.0f);
        EXPLANATION_TITLE = sectionTitle(ColorManager.ACCENT_BLUE_LIGHT);
        ANALOGY_TITLE = sectionTitle(ColorManager.SYNTAX_CLASS);
        WORLD_TITLE = sectionTitle(ColorManager.SUCCESS_GREEN);
        CODE_TITLE = sectionTitle(ColorManager.SYNTAX_KEYWORD);
        FLOW_TITLE = sectionTitle(ColorManager.SYNTAX_CLASS);
        MISTAKES_TITLE = sectionTitle(ColorManager.ERROR_RED);
        NEXT_STEP_TITLE = sectionTitle(ColorManager.ACCENT_BLUE_LIGHT);
        REFERENCE_TITLE = sectionTitle(ColorManager.TEXT_TERTIARY);
        BODY = create(bodyFont(), ColorManager.TEXT_SECONDARY, null, 0, 9, BODY_LEFT_INDENT, BODY_RIGHT_INDENT, BODY_LINE_SPACING);
        CODE = create(codeFont(), ColorManager.EDITOR_FOREGROUND, null, 0, 0, CODE_LEFT_INDENT, CODE_RIGHT_INDENT, CODE_LINE_SPACING);
        BULLET = create(bulletFont(), ColorManager.TEXT_PRIMARY, null, 0, 7, BULLET_LEFT_INDENT, BULLET_RIGHT_INDENT, BULLET_LINE_SPACING);
        MISTAKE = create(bulletFont(), ColorManager.TEXT_PRIMARY, null, 0, 7, BULLET_LEFT_INDENT, BULLET_RIGHT_INDENT, BULLET_LINE_SPACING);
        LINK = create(linkFont(), ColorManager.ACCENT_BLUE_LIGHT, null, 12, 6, TITLE_LEFT_INDENT, TITLE_RIGHT_INDENT, 0.0f);
        ARROW = create(arrowFont(), ColorManager.TEXT_TERTIARY, null, 0, 0, 0, 0, 0.0f);
        DIVIDER = create(bodyFont(), ColorManager.BORDER_CARD, null, 18, 18, 0, 0, 0.0f);
        CODE_KEYWORD = syntax(ColorManager.SYNTAX_KEYWORD, true);
        CODE_TYPE = syntax(ColorManager.SYNTAX_CLASS, false);
        CODE_STRING = syntax(ColorManager.SYNTAX_STRING, false);
        CODE_COMMENT = syntax(ColorManager.TEXT_MUTED, false);
        CODE_PARAGRAPH = new SimpleAttributeSet();
        StyleConstants.setBackground(CODE_PARAGRAPH, ColorManager.EDITOR_BG);
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

    public static SimpleAttributeSet explanationTitle() {
        return copy(EXPLANATION_TITLE);
    }

    public static SimpleAttributeSet analogyTitle() {
        return copy(ANALOGY_TITLE);
    }

    public static SimpleAttributeSet worldTitle() {
        return copy(WORLD_TITLE);
    }

    public static SimpleAttributeSet codeTitle() {
        return copy(CODE_TITLE);
    }

    public static SimpleAttributeSet flowTitle() {
        return copy(FLOW_TITLE);
    }

    public static SimpleAttributeSet mistakesTitle() {
        return copy(MISTAKES_TITLE);
    }

    public static SimpleAttributeSet nextStepTitle() {
        return copy(NEXT_STEP_TITLE);
    }

    public static SimpleAttributeSet referenceTitle() {
        return copy(REFERENCE_TITLE);
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

    public static SimpleAttributeSet mistake() {
        return copy(MISTAKE);
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

    public static SimpleAttributeSet codeKeyword() {
        return copy(CODE_KEYWORD);
    }

    public static SimpleAttributeSet codeType() {
        return copy(CODE_TYPE);
    }

    public static SimpleAttributeSet codeString() {
        return copy(CODE_STRING);
    }

    public static SimpleAttributeSet codeComment() {
        return copy(CODE_COMMENT);
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

    public static int codeBlockHorizontalPadding() {
        return CODE_BLOCK_HORIZONTAL_PADDING;
    }

    public static int codeBlockMinimumColumns() {
        return CODE_BLOCK_MINIMUM_COLUMNS;
    }

    public static SimpleAttributeSet codeParagraph() {
        return copy(CODE_PARAGRAPH);
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
        return TypographyManager.monoRegular(CODE_FONT_SIZE);
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

    private static SimpleAttributeSet sectionTitle(Color color) {
        return create(sectionTitleFont(), color, null, 18, 10, TITLE_LEFT_INDENT, TITLE_RIGHT_INDENT, 0.0f);
    }

    private static SimpleAttributeSet syntax(Color color, boolean bold) {
        SimpleAttributeSet attrs = new SimpleAttributeSet();
        StyleConstants.setFontFamily(attrs, codeFont().getFamily());
        StyleConstants.setFontSize(attrs, codeFont().getSize());
        StyleConstants.setBold(attrs, bold);
        StyleConstants.setForeground(attrs, color);
        return attrs;
    }

    private static SimpleAttributeSet copy(SimpleAttributeSet source) {
        return new SimpleAttributeSet(source);
    }
}
