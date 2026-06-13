package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class ToolWindowButton extends JButton {

    private boolean selected;

    public ToolWindowButton(Icon icon, String text) {
        super(text, icon);

        setVerticalTextPosition(SwingConstants.BOTTOM);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setHorizontalAlignment(SwingConstants.CENTER);
        setVerticalAlignment(SwingConstants.CENTER);
        
        setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        setIconTextGap(4);
        setMargin(new Insets(0, 0, 0, 0));

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(new Color(187, 187, 187));
        setPreferredSize(new Dimension(64, 56));
        setMaximumSize(new Dimension(64, 56));
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
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (selected) {
            g2.setColor(new Color(53, 116, 240));
            g2.fillRoundRect(6, 4, getWidth() - 12, getHeight() - 8, 8, 8);
            setForeground(Color.WHITE);
        } else if (getModel().isRollover()) {
            g2.setColor(new Color(255, 255, 255, 20));
            g2.fillRoundRect(6, 4, getWidth() - 12, getHeight() - 8, 8, 8);
            setForeground(new Color(220, 220, 220));
        } else {
            setForeground(new Color(187, 187, 187));
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
