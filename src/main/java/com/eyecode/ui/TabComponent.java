package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TabComponent extends JPanel {

    private boolean selected;

    public TabComponent(JTabbedPane tabbedPane, String title) {

        setOpaque(false);

        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));


        JLabel titleLabel = new JLabel(title);

        titleLabel.setForeground(new Color(220, 220, 220));

        titleLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));


        JButton closeButton = new JButton("✕");

        closeButton.setBorderPainted(false);

        closeButton.setContentAreaFilled(false);

        closeButton.setFocusPainted(false);

        closeButton.setForeground(new Color(160, 160, 160));

        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        closeButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(new Color(255, 80, 80));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(new Color(160, 160, 160));
            }
        });

        closeButton.addActionListener(e -> {

            int index = tabbedPane.indexOfTabComponent(TabComponent.this);

            if (index != -1) {
                tabbedPane.remove(index);
            }
        });

        add(titleLabel);
        add(closeButton);

    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D)  g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (selected) {

            g2.setColor(new Color(45, 45, 48));

            g2.fillRoundRect(
                    0,
                    0,
                    getWidth(),
                    getHeight(),
                    10,
                    10
            );
        }

        g2.dispose();

        super.paintComponent(g);

    }
}
