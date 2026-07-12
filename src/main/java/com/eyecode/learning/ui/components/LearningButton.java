package com.eyecode.learning.ui.components;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import java.awt.*;

public final class LearningButton extends JButton {

    private static final int FONT_SIZE = 12;

    public LearningButton(String text) {
        super(text);
        setFont(TypographyManager.monoRegular(FONT_SIZE));
        setForeground(ColorManager.ACCENT_BLUE_LIGHT);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setFocusPainted(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 0));
    }
}
