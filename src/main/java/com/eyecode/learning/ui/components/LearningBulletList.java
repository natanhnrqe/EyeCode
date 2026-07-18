package com.eyecode.learning.ui.components;

import com.eyecode.ui.core.UIViewFactory;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.swing.SwingUIViewFactory;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import java.awt.Component;
import java.util.List;

public final class LearningBulletList extends JPanel {

    private static final int FONT_SIZE = 12;

    public LearningBulletList(List<String> items) {
        this(items, new SwingUIViewFactory());
    }

    public LearningBulletList(List<String> items, UIViewFactory viewFactory) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setAlignmentX(Component.LEFT_ALIGNMENT);
        setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));

        if (items == null) return;

        for (String item : items) {
            var label = viewFactory.createLabel();
            label.getLabel().setText("\u2022 " + item);
            label.getLabel().setFont(TypographyManager.monoRegular(FONT_SIZE));
            label.getLabel().setForeground(ColorManager.TEXT_SECONDARY);
            label.getLabel().setAlignmentX(Component.LEFT_ALIGNMENT);
            label.getLabel().setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
            add(label.getLabel());
        }
    }
}
