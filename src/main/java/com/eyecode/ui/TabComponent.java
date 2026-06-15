package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TabComponent extends JPanel {

    private final JLabel titleLabel;
    private boolean selected;

    public TabComponent(String title, Runnable onClose) {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, SpacingSystem.SM, 0));

        titleLabel = new JLabel(title);
        titleLabel.setForeground(ColorManager.TEXT_PRIMARY);
        titleLabel.setFont(TypographyManager.UI_TAB());

        JButton closeButton = new JButton(IconManager.close());
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setForeground(ColorManager.TEXT_TAB_CLOSE);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setPreferredSize(new Dimension(SpacingSystem.TAB_CLOSE_SIZE, SpacingSystem.TAB_CLOSE_SIZE));
        closeButton.setToolTipText("Close tab");

        closeButton.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseEntered(MouseEvent e) {
                closeButton.setForeground(ColorManager.TAB_HOVER_RED);
            }

            @Override
            public void mouseExited(MouseEvent e) {
                closeButton.setForeground(ColorManager.TEXT_TAB_CLOSE);
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
            g2.setColor(ColorManager.SELECTED_TAB_BG);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
