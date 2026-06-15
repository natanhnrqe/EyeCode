package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
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

        setFont(TypographyManager.UI_TOOLWINDOW());
        setIconTextGap(SpacingSystem.XS);
        setMargin(new Insets(0, 0, 0, 0));

        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setOpaque(false);
        setForeground(ColorManager.TEXT_SECONDARY);
        setPreferredSize(new Dimension(UIConstants.TOOLWINDOW_WIDTH, UIConstants.TOOLWINDOW_BTN_H));
        setMaximumSize(new Dimension(UIConstants.TOOLWINDOW_WIDTH, UIConstants.TOOLWINDOW_BTN_H));
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
            g2.setColor(ColorManager.ACCENT_BLUE_LIGHT);
            g2.fillRoundRect(6, 4, getWidth() - 12, getHeight() - 8, 8, 8);
            setForeground(Color.WHITE);
        } else if (getModel().isRollover()) {
            g2.setColor(ColorManager.HOVER_OVERLAY);
            g2.fillRoundRect(6, 4, getWidth() - 12, getHeight() - 8, 8, 8);
            setForeground(ColorManager.TEXT_PRIMARY);
        } else {
            setForeground(ColorManager.TEXT_SECONDARY);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
