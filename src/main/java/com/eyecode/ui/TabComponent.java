package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TabComponent extends JPanel {

    private final JLabel titleLabel;
    private boolean selected;

    public TabComponent(String title, Runnable onClose) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 6, 0));

        titleLabel = new JLabel(title);
        titleLabel.setForeground(new Color(220, 220, 220));
        titleLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));

        JButton closeButton = new JButton(UiIcons.close());
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setForeground(new Color(160, 160, 160));
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setPreferredSize(new Dimension(22, 22));
        closeButton.setToolTipText("Close tab");

        closeButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(new Color(255, 90, 90));
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(new Color(160, 160, 160));
            }
        });

        closeButton.addActionListener(e -> {
            if (onClose != null) {
                onClose.run();
            }
        });

        add(titleLabel);
        add(closeButton);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (selected) {
            g2.setColor(new Color(45, 45, 48));
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
