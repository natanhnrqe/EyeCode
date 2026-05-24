package com.eyecode.ui;

import com.jediterm.terminal.TextStyle;
import com.jediterm.terminal.emulator.ColorPalette;
import com.jediterm.terminal.ui.settings.DefaultSettingsProvider;


import java.awt.*;

public class EyeCodeTerminalSettings extends DefaultSettingsProvider {

    @Override
    public Font getTerminalFont() {

        return new Font("JetBrains Mono", Font.PLAIN, 14);

    }

    @Override
    public float getTerminalFontSize() {
        return 14;
    }

//    @Override
//    public ColorPalette getTerminalColorPalette() {
//
//
//    }

    @Override
    public TextStyle getDefaultStyle() {

        return new TextStyle(TerminalTheme.FOREGROUND, TerminalTheme.BACKGROUND);
    }
}
