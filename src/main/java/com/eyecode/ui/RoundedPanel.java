package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class RoundedPanel extends JPanel {

    private final int arc;
    private final Color borderColor;

    public RoundedPanel() {
        this(new BorderLayout(), new Color(35, 37, 41), new Color(54, 57, 63), 14);
    }

    public RoundedPanel(LayoutManager layout) {
        this(layout, new Color(35, 37, 41), new Color(54, 57, 63), 14);
    }

    public RoundedPanel(LayoutManager layout, Color background, Color borderColor, int arc) {
        super(layout);
        this.arc = arc;
        this.borderColor = borderColor;
        setBackground(background);
        setOpaque(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.setColor(borderColor);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);

        g2.dispose();
        super.paintComponent(g);
    }
}
