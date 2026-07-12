package com.eyecode.learning.ui.components;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import java.awt.*;

public final class LearningTitle extends JLabel {

    private static final int FONT_SIZE = 16;

    public LearningTitle(String text, Icon icon) {
        super(text, icon, LEADING);
        setFont(TypographyManager.monoBold(FONT_SIZE));
        setForeground(ColorManager.TEXT_PRIMARY);
        setIconTextGap(8);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }

    public LearningTitle(String text) {
        this(text, null);
    }
}
