package com.eyecode.learning.ui.components;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public final class LearningBulletList extends JPanel {

    private static final int FONT_SIZE = 12;

    public LearningBulletList(List<String> items) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));

        if (items == null) return;

        for (String item : items) {
            JLabel bullet = new JLabel("\u2022 " + item);
            bullet.setFont(TypographyManager.monoRegular(FONT_SIZE));
            bullet.setForeground(ColorManager.TEXT_SECONDARY);
            bullet.setAlignmentX(Component.LEFT_ALIGNMENT);
            bullet.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            add(bullet);
        }
    }
}
