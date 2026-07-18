package com.eyecode.learning.ui.components;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.*;
import java.awt.*;

public final class LearningCodeBlock extends JPanel {

    private static final int FONT_SIZE = 13;
    private static final int ARC = 6;
    private static final int PADDING = 10;

    private final JTextArea area;

    public LearningCodeBlock(String code) {
        setLayout(new BorderLayout());
        setOpaque(false);

        area = new JTextArea(code);
        area.setFont(TypographyManager.monoRegular(FONT_SIZE));
        area.setForeground(ColorManager.EDITOR_FOREGROUND);
        area.setBackground(ColorManager.EDITOR_BG);
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(BorderFactory.createEmptyBorder(PADDING, PADDING, PADDING, PADDING));
        area.setOpaque(true);

        add(area, BorderLayout.CENTER);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(ColorManager.EDITOR_BG);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), ARC, ARC);
        g2.setColor(ColorManager.BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, ARC, ARC);
        g2.dispose();
        super.paintComponent(g);
    }

    @Override
    public Dimension getMaximumSize() {
        return new Dimension(Integer.MAX_VALUE, super.getPreferredSize().height);
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        d.width = Math.max(d.width, area.getPreferredSize().width + PADDING * 2);
        return d;
    }
}
