package com.eyecode.learning.ui.components;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import java.awt.*;

public final class LearningSubtitle extends JLabel {

    private static final int FONT_SIZE = 11;

    public LearningSubtitle(String text) {
        super(text);
        setFont(TypographyManager.monoRegular(FONT_SIZE));
        setForeground(ColorManager.TEXT_SECONDARY);
        setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
    }
}
