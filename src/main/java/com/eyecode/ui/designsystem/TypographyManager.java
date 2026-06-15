package com.eyecode.ui.designsystem;

import java.awt.Font;
import java.awt.GraphicsEnvironment;

public final class TypographyManager {

    private static final String FONT_NAME_MONO = "JetBrains Mono";
    private static final String FONT_FALLBACK  = "Consolas";

    private TypographyManager() {}

    public static Font mono(int size, int style) {
        return new Font(resolveMono(), style, size);
    }

    public static Font monoRegular(int size) {
        return mono(size, Font.PLAIN);
    }

    public static Font monoBold(int size) {
        return mono(size, Font.BOLD);
    }

    // Common presets
    public static Font UI_SMALL()       { return monoRegular(10); }
    public static Font UI_LABEL()       { return monoRegular(12); }
    public static Font UI_BODY()        { return monoRegular(13); }
    public static Font UI_BOLD()        { return monoBold(12); }
    public static Font UI_TITLE()       { return monoBold(14); }
    public static Font UI_LOGO()        { return monoBold(14); }
    public static Font UI_WELCOME()     { return monoBold(34); }
    public static Font UI_WELCOME_SUB() { return monoRegular(14); }
    public static Font UI_TAB()         { return monoRegular(13); }
    public static Font UI_BUTTON()      { return monoRegular(13); }
    public static Font UI_TOOLWINDOW()  { return monoRegular(10); }
    public static Font UI_TREE()        { return monoRegular(13); }
    public static Font UI_PATH()        { return monoRegular(12); }
    public static Font UI_STATUS()      { return monoRegular(12); }
    public static Font UI_CODE()        { return monoRegular(13); }
    public static Font UI_RUN_OUTPUT()  { return monoRegular(13); }
    public static Font UI_DEFAULT()     { return monoRegular(13); }

    private static String resolveMono() {
        for (String name : GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames()) {
            if (name.equalsIgnoreCase(FONT_NAME_MONO)) return FONT_NAME_MONO;
        }
        return FONT_FALLBACK;
    }
}
