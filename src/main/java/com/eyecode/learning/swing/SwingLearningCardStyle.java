package com.eyecode.learning.swing;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

public final class SwingLearningCardStyle {

    private SwingLearningCardStyle() {}

    // ── Card surface ───────────────────────────────────────
    public static final Color CARD_BACKGROUND   = ColorManager.CARD_BG;
    public static final Color CARD_BORDER       = ColorManager.BORDER_CARD;
    public static final int    CARD_BORDER_WIDTH = 1;

    public static final int CARD_PADDING_TOP    = 8;
    public static final int CARD_PADDING_LEFT   = 12;
    public static final int CARD_PADDING_BOTTOM = 8;
    public static final int CARD_PADDING_RIGHT  = 12;

    public static final int CARD_MIN_WIDTH  = 280;
    public static final int CARD_MAX_WIDTH  = 460;
    public static final int CARD_MIN_HEIGHT = 80;
    public static final int CARD_MAX_HEIGHT = 480;

    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(CARD_BORDER, CARD_BORDER_WIDTH),
                BorderFactory.createEmptyBorder(
                        CARD_PADDING_TOP, CARD_PADDING_LEFT,
                        CARD_PADDING_BOTTOM, CARD_PADDING_RIGHT));
    }

    // ── Header ────────────────────────────────────────────
    public static final int HEADER_ICON_SIZE        = 16;
    public static final int HEADER_TITLE_SIZE       = 15;
    public static final int HEADER_SUBTITLE_SIZE    = 11;
    public static final int HEADER_ICON_TITLE_GAP   = 8;
    public static final int HEADER_SUBTITLE_INDENT  = 22;
    public static final int HEADER_PADDING_TOP      = 4;
    public static final int HEADER_PADDING_LEFT     = 2;
    public static final int HEADER_PADDING_BOTTOM   = 6;
    public static final int HEADER_PADDING_RIGHT    = 2;
    public static final int HEADER_SUBTITLE_TOP_GAP = 2;
    public static final int HEADER_DIVIDER_THICKNESS = 1;

    public static final Color HEADER_TITLE_COLOR    = ColorManager.TEXT_PRIMARY;
    public static final Color HEADER_SUBTITLE_COLOR = ColorManager.TEXT_MUTED;
    public static final Color HEADER_DIVIDER_COLOR = ColorManager.BORDER_DIVIDER;

    public static Font headerTitleFont() {
        return TypographyManager.monoBold(HEADER_TITLE_SIZE);
    }

    public static Font headerSubtitleFont() {
        return TypographyManager.monoRegular(HEADER_SUBTITLE_SIZE);
    }

    public static Border headerBorder() {
        return BorderFactory.createEmptyBorder(
                HEADER_PADDING_TOP, HEADER_PADDING_LEFT,
                HEADER_PADDING_BOTTOM, HEADER_PADDING_RIGHT);
    }

    // ── Action bar ────────────────────────────────────────
    public static final int ACTION_BAR_PADDING_TOP    = 4;
    public static final int ACTION_BAR_PADDING_BOTTOM = 6;
    public static final int ACTION_BAR_BUTTON_GAP     = 8;
    public static final int ACTION_BAR_BUTTON_TOP_PX    = 3;
    public static final int ACTION_BAR_BUTTON_LEFT_PX   = 8;
    public static final int ACTION_BAR_BUTTON_BOTTOM_PX = 3;
    public static final int ACTION_BAR_BUTTON_RIGHT_PX  = 8;
    public static final int ACTION_BAR_BUTTON_FONT_SIZE = 11;

    public static final Color ACTION_BAR_FG_ENABLED  = ColorManager.TEXT_TERTIARY;
    public static final Color ACTION_BAR_FG_DISABLED = ColorManager.TEXT_DISABLED;
    public static final Color ACTION_BAR_FG_HOVER    = ColorManager.TEXT_PRIMARY;
    public static final Color ACTION_BAR_BG_NORMAL   = new Color(0, 0, 0, 0);
    public static final Color ACTION_BAR_BG_HOVER    = ColorManager.ACCENT_HOVER_BG;
    public static final Color ACTION_BAR_BG_PRESSED  = ColorManager.ACCENT_SELECTION;
    public static final Color ACTION_BAR_BORDER      = ColorManager.BORDER;

    public static Font actionBarButtonFont() {
        return TypographyManager.monoRegular(ACTION_BAR_BUTTON_FONT_SIZE);
    }

    public static Border actionBarButtonBorder() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ACTION_BAR_BORDER, 1),
                BorderFactory.createEmptyBorder(
                        ACTION_BAR_BUTTON_TOP_PX, ACTION_BAR_BUTTON_LEFT_PX,
                        ACTION_BAR_BUTTON_BOTTOM_PX, ACTION_BAR_BUTTON_RIGHT_PX));
    }

    // ── Body ──────────────────────────────────────────────
    public static final int BODY_PADDING_TOP    = 12;
    public static final int BODY_PADDING_LEFT    = 14;
    public static final int BODY_PADDING_BOTTOM  = 12;
    public static final int BODY_PADDING_RIGHT   = 14;
    public static final int BODY_HEADING_SIZE     = 13;
    public static final int BODY_PARAGRAPH_SIZE  = 12;
    public static final int BODY_BULLET_SIZE      = 12;
    public static final int BODY_HEADING_TOP_GAP     = 12;
    public static final int BODY_HEADING_BOTTOM_GAP  = 4;
    public static final int BODY_PARAGRAPH_BOTTOM_GAP = 12;
    public static final int BODY_CODE_BOTTOM_GAP     = 12;
    public static final int BODY_BULLET_TOP_GAP     = 2;
    public static final int BODY_BULLET_BOTTOM_GAP  = 2;
    public static final int BODY_BULLET_INDENT      = 16;

    public static final Color BODY_HEADING_COLOR     = ColorManager.TEXT_PRIMARY;
    public static final Color BODY_PARAGRAPH_COLOR   = ColorManager.TEXT_SECONDARY;
    public static final Color BODY_BULLET_COLOR      = ColorManager.TEXT_PRIMARY;
    public static final Color BODY_BULLET_MARKER     = ColorManager.TEXT_MUTED;
    public static final Color BODY_VIEWPORT_BG       = ColorManager.CARD_BG;

    public static Font bodyHeadingFont() {
        return TypographyManager.monoBold(BODY_HEADING_SIZE);
    }

    public static Font bodyParagraphFont() {
        return TypographyManager.monoRegular(BODY_PARAGRAPH_SIZE);
    }

    public static Font bodyBulletFont() {
        return TypographyManager.monoRegular(BODY_BULLET_SIZE);
    }

    public static Border bodyContentBorder() {
        return BorderFactory.createEmptyBorder(
                BODY_PADDING_TOP, BODY_PADDING_LEFT,
                BODY_PADDING_BOTTOM, BODY_PADDING_RIGHT);
    }

    // ── Code block ───────────────────────────────────────
    public static final int CODE_HEADER_LABEL_SIZE   = 11;
    public static final int CODE_BUTTON_FONT_SIZE    = 11;
    public static final int CODE_TEXT_SIZE           = 12;
    public static final int CODE_AREA_PADDING_TOP    = 10;
    public static final int CODE_AREA_PADDING_LEFT   = 12;
    public static final int CODE_AREA_PADDING_BOTTOM = 10;
    public static final int CODE_AREA_PADDING_RIGHT  = 12;
    public static final int CODE_HEADER_PADDING_TOP    = 5;
    public static final int CODE_HEADER_PADDING_LEFT   = 10;
    public static final int CODE_HEADER_PADDING_BOTTOM = 5;
    public static final int CODE_HEADER_PADDING_RIGHT  = 10;
    public static final int CODE_MAX_VISIBLE_HEIGHT   = 200;

    public static final Color CODE_BORDER            = ColorManager.BORDER_CARD;
    public static final Color CODE_BG                = ColorManager.EDITOR_BG;
    public static final Color CODE_HEADER_BG         = ColorManager.PANEL_BG;
    public static final Color CODE_TEXT_COLOR        = ColorManager.EDITOR_FOREGROUND;
    public static final Color CODE_LANGUAGE_COLOR    = ColorManager.TEXT_MUTED;
    public static final Color CODE_BUTTON_FG         = ColorManager.TEXT_MUTED;

    public static Font codeHeaderTextFont() {
        return TypographyManager.monoRegular(CODE_HEADER_LABEL_SIZE);
    }

    public static Font codeButtonFont() {
        return TypographyManager.monoRegular(CODE_BUTTON_FONT_SIZE);
    }

    public static Font codeTextFont() {
        return TypographyManager.monoRegular(CODE_TEXT_SIZE);
    }

    public static Border codeAreaBorder() {
        return BorderFactory.createEmptyBorder(
                CODE_AREA_PADDING_TOP, CODE_AREA_PADDING_LEFT,
                CODE_AREA_PADDING_BOTTOM, CODE_AREA_PADDING_RIGHT);
    }

    // ── Footer ────────────────────────────────────────────
    public static final int FOOTER_TOP_GAP     = 6;
    public static final int FOOTER_PADDING_TOP    = 6;
    public static final int FOOTER_PADDING_BOTTOM = 8;
    public static final int FOOTER_LABEL_GAP = 4;
    public static final int FOOTER_FONT_SIZE    = 10;

    public static final Color FOOTER_LABEL_COLOR  = ColorManager.TEXT_MUTED;
    public static final Color FOOTER_VALUE_COLOR  = ColorManager.TEXT_MUTED;
    public static final Color FOOTER_DIVIDER_COLOR = ColorManager.BORDER_DIVIDER;
    public static final int    FOOTER_DIVIDER_THICKNESS = 1;

    public static Font footerFont() {
        return TypographyManager.monoRegular(FOOTER_FONT_SIZE);
    }

    // ── Scrollbar ────────────────────────────────────────
    public static final int    SCROLLBAR_WIDTH     = 8;
    public static final Color SCROLLBAR_TRACK       = new Color(0, 0, 0, 0);
    public static final Color SCROLLBAR_THUMB       = new Color(80, 80, 86);
    public static final Color SCROLLBAR_THUMB_HOVER = new Color(102, 102, 110);
    public static final Color SCROLLBAR_THUMB_PRESSED = new Color(122, 122, 130);

    public static Dimension cardPreferredSize(Dimension natural) {
        int width = Math.max(CARD_MIN_WIDTH, Math.min(CARD_MAX_WIDTH, natural.width));
        int height = Math.max(CARD_MIN_HEIGHT, Math.min(CARD_MAX_HEIGHT, natural.height));
        return new Dimension(width, height);
    }

    public static Insets zeroInsets() {
        return new Insets(0, 0, 0, 0);
    }
}
