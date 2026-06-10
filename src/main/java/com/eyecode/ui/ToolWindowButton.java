package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class ToolWindowButton extends JButton {

    private boolean selected;

    public ToolWindowButton(Icon icon) {
        super(icon);

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(new Color(187, 187, 187));
        setPreferredSize(new Dimension(48, 40));
        setMaximumSize(new Dimension(48, 40));
        setAlignmentX(Component.CENTER_ALIGNMENT);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }

    public void setSelectedState(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();

        if (selected) {
            g2.setColor(new Color(74, 136, 255));
            g2.fillRoundRect(0, 6, 3, getHeight() - 12, 4, 4);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
