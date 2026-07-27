package com.eyecode.learning.swing;

import javax.swing.JButton;
import javax.swing.SwingConstants;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class FlatActionBarButton extends JButton {

    private Color bgNormal  = SwingLearningCardStyle.ACTION_BAR_BG_NORMAL;
    private Color bgHover   = SwingLearningCardStyle.ACTION_BAR_BG_HOVER;
    private Color bgPressed = SwingLearningCardStyle.ACTION_BAR_BG_PRESSED;
    private Color bgCurrent = bgNormal;
    private boolean hover = false;

    FlatActionBarButton(String text) {
        super(text);
        setFont(SwingLearningCardStyle.actionBarButtonFont());
        setForeground(SwingLearningCardStyle.ACTION_BAR_FG_ENABLED);
        setBackground(null);
        setBorder(SwingLearningCardStyle.actionBarButtonBorder());
        setContentAreaFilled(false);
        setFocusable(false);
        setBorderPainted(true);
        setVerticalAlignment(SwingConstants.CENTER);
        setHorizontalTextPosition(SwingConstants.CENTER);
        setHorizontalAlignment(SwingConstants.CENTER);
        setIconTextGap(0);

        addMouseListener(new MouseAdapter() {
            @Override public void mouseEntered(MouseEvent e) {
                if (isEnabled()) {
                    hover = true;
                    bgCurrent = bgHover;
                    setForeground(SwingLearningCardStyle.ACTION_BAR_FG_HOVER);
                    repaint();
                }
            }

            @Override public void mouseExited(MouseEvent e) {
                if (isEnabled()) {
                    hover = false;
                    bgCurrent = bgNormal;
                    setForeground(SwingLearningCardStyle.ACTION_BAR_FG_ENABLED);
                    repaint();
                }
            }

            @Override public void mousePressed(MouseEvent e) {
                if (isEnabled()) {
                    bgCurrent = bgPressed;
                    repaint();
                }
            }

            @Override public void mouseReleased(MouseEvent e) {
                if (isEnabled()) {
                    bgCurrent = hover ? bgHover : bgNormal;
                    repaint();
                }
            }
        });
    }

    @Override
    public void updateUI() {
        super.updateUI();
        setContentAreaFilled(false);
        setFocusable(false);
        setBorderPainted(true);
    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        if (!enabled) {
            setForeground(SwingLearningCardStyle.ACTION_BAR_FG_DISABLED);
            bgCurrent = bgNormal;
        } else {
            setForeground(SwingLearningCardStyle.ACTION_BAR_FG_ENABLED);
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if (bgCurrent != null) {
                g2.setColor(bgCurrent);
                g2.fillRect(0, 0, getWidth(), getHeight());
            }
        } finally {
            g2.dispose();
        }
        super.paintComponent(g);
    }

    public Color getBgNormal() { return bgNormal; }
    public void setBgNormal(Color c) { this.bgNormal = c; }
    public void setBgHover(Color c) { this.bgHover = c; }
    public void setBgPressed(Color c) { this.bgPressed = c; }

    @Override
    public Color getBackground() {
        return bgCurrent;
    }

    @Override
    public boolean isOpaque() {
        return false;
    }
}
