package com.eyecode.ui.scroll;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import java.awt.*;

public class ModernScrollBarUI extends BasicScrollBarUI {

    @Override
    protected Dimension getMinimumThumbSize() {

        return new Dimension(8, 8);
    }

    @Override
    protected void paintTrack(Graphics g, JComponent c, Rectangle trackBounds) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setColor(new Color(30, 30, 30));

        g2.fillRect(
                trackBounds.x,
                trackBounds.y,
                trackBounds.width,
                trackBounds.height
        );

        g2.dispose();
    }

    @Override
    protected void paintThumb(Graphics g, JComponent c, Rectangle thumbBounds) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(new Color(90, 90, 90));

        g2.fillRoundRect(
                thumbBounds.x + 2,
                thumbBounds.y + 2,
                thumbBounds.width - 4,
                thumbBounds.height - 4,
                10,
                10);

        g2.dispose();
    }

    @Override
    protected JButton createDecreaseButton(int orientation) {

        return createZeroButton();
    }

    @Override
    protected JButton createIncreaseButton(int orientation) {


        return createZeroButton();
    }

    private JButton createZeroButton() {

        JButton jButton = new JButton();

        jButton.setPreferredSize(new Dimension(0, 0));

        jButton.setMinimumSize(new Dimension(0, 0));

        jButton.setMaximumSize(new Dimension(0, 0));

        return jButton;
    }
}
