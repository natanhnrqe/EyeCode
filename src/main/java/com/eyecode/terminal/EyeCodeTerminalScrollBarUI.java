package com.eyecode.terminal;

import com.eyecode.ui.TerminalTheme;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

public final class EyeCodeTerminalScrollBarUI extends BasicScrollBarUI {
    @Override
    public Dimension getPreferredSize(JComponent component) {
        return new Dimension(10, 10);
    }

    @Override
    protected Dimension getMinimumThumbSize() {
        return new Dimension(8, 8);
    }

    @Override
    protected void paintTrack(Graphics graphics, JComponent component, Rectangle bounds) {
        graphics.setColor(TerminalTheme.SWING_BACKGROUND);
        graphics.fillRect(bounds.x, bounds.y, bounds.width, bounds.height);
    }

    @Override
    protected void paintThumb(Graphics graphics, JComponent component, Rectangle bounds) {
        Graphics2D graphics2d = (Graphics2D) graphics.create();
        graphics2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2d.setColor(TerminalTheme.SCROLLBAR_THUMB);
        graphics2d.fillRoundRect(bounds.x + 2, bounds.y + 2,
                Math.max(0, bounds.width - 4), Math.max(0, bounds.height - 4), 8, 8);
        graphics2d.dispose();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {
        return hiddenButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {
        return hiddenButton();
    }

    private JButton hiddenButton() {
        JButton button = new JButton();
        button.setPreferredSize(new Dimension());
        button.setMinimumSize(new Dimension());
        button.setMaximumSize(new Dimension());
        return button;
    }
}