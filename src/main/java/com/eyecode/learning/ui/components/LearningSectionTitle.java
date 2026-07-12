package com.eyecode.learning.ui.components;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import java.awt.*;

public final class LearningSectionTitle extends JLabel {

    private static final int FONT_SIZE = 12;

    public LearningSectionTitle(String text) {
        super(text);
        setFont(TypographyManager.monoBold(FONT_SIZE));
        setForeground(ColorManager.SYNTAX_CLASS);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }
}
