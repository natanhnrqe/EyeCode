package com.eyecode.ui.designsystem;

import java.awt.Color;

public final class ColorManager {

    private ColorManager() {}

    // ── Backgrounds ─────────────────────────────────────────
    public static final Color WINDOW_BG         = new Color(24, 25, 28);
    public static final Color EDITOR_BG         = new Color(25, 26, 28);
    public static final Color PANEL_BG          = new Color(30, 30, 30);
    public static final Color TOOLBAR_BG        = new Color(37, 37, 38);
    public static final Color SURFACE_BG        = new Color(45, 45, 48);
    public static final Color CARD_BG           = new Color(49, 51, 56);
    public static final Color INPUT_BG          = new Color(45, 45, 45);
    public static final Color BREADCRUMB_BG     = new Color(43, 45, 48);
    public static final Color LINE_NUMBER_BG    = new Color(39, 41, 42);
    public static final Color LINE_NUMBER_ROW_BG = new Color(30, 30, 30);
    public static final Color SELECTED_TAB_BG   = new Color(45, 45, 48);

    // ── Borders ─────────────────────────────────────────────
    public static final Color BORDER            = new Color(55, 58, 64);
    public static final Color BORDER_DIVIDER    = new Color(48, 51, 57);
    public static final Color BORDER_CARD       = new Color(67, 70, 76);
    public static final Color BORDER_EDITOR     = new Color(54, 57, 63);

    // ── Text ────────────────────────────────────────────────
    public static final Color TEXT_PRIMARY      = new Color(220, 220, 220);
    public static final Color TEXT_SECONDARY    = new Color(187, 187, 187);
    public static final Color TEXT_TERTIARY     = new Color(150, 155, 165);
    public static final Color TEXT_MUTED        = new Color(145, 150, 160);
    public static final Color TEXT_DISABLED     = new Color(105, 105, 105);
    public static final Color TEXT_FILE_TREE    = new Color(169, 183, 198);
    public static final Color TEXT_TAB_CLOSE    = new Color(160, 160, 160);

    // ── Accent ──────────────────────────────────────────────
    public static final Color ACCENT_BLUE       = new Color(40, 94, 184);
    public static final Color ACCENT_BLUE_LIGHT = new Color(53, 116, 240);
    public static final Color ACCENT_HOVER_BG   = new Color(58, 61, 67);
    public static final Color ACCENT_SELECTION  = new Color(68, 71, 74);

    // ── Status ──────────────────────────────────────────────
    public static final Color SUCCESS_GREEN     = new Color(42, 130, 73);
    public static final Color RUNNING_GREEN     = new Color(87, 166, 74);
    public static final Color ERROR_RED         = new Color(196, 43, 28);
    public static final Color BREAKPOINT_RED    = new Color(255, 85, 85);
    public static final Color TAB_HOVER_RED     = new Color(255, 90, 90);
    public static final Color STATUS_READY      = new Color(87, 166, 74);
    public static final Color STATUS_BUSY       = new Color(255, 190, 50);

    // ── Editor ──────────────────────────────────────────────
    public static final Color EDITOR_FOREGROUND = new Color(188, 190, 196);
    public static final Color EDITOR_SELECTION  = new Color(33, 66, 131);
    public static final Color EDITOR_CARET      = Color.WHITE;
    public static final Color LINE_NUMBER_FG    = new Color(128, 128, 128);

    // ── Syntax highlighting ─────────────────────────────────
    public static final Color SYNTAX_TYPE       = new Color(78, 201, 176);
    public static final Color SYNTAX_CLASS      = new Color(78, 201, 176);
    public static final Color SYNTAX_ANNOTATION = new Color(187, 181, 41);
    public static final Color SYNTAX_CONSTANT   = new Color(199, 125, 187);
    public static final Color SYNTAX_KEYWORD    = new Color(207, 109, 100);
    public static final Color SYNTAX_STRING     = new Color(106, 171, 115);
    public static final Color SYNTAX_COMMENT    = new Color(122, 126, 133);
    public static final Color SYNTAX_NUMBER     = new Color(42, 172, 184);
    public static final Color SYNTAX_METHOD     = new Color(86, 168, 245);

    // ── Autocomplete ────────────────────────────────────────
    public static final Color AUTOCOMPLETE_BG            = new Color(43, 43, 43);
    public static final Color AUTOCOMPLETE_FG            = new Color(169, 183, 198);
    public static final Color AUTOCOMPLETE_SELECTION_BG  = new Color(75, 110, 175);

    // ── Breakpoints ─────────────────────────────────────────
    public static final Color BREAKPOINT_LINE_BG = new Color(70, 15, 40);

    // ── Dividers ────────────────────────────────────────────
    public static final Color DIVIDER_COLOR     = new Color(60, 60, 60);

    // ── Overlays ────────────────────────────────────────────
    public static final Color HOVER_OVERLAY     = new Color(255, 255, 255, 20);
}
