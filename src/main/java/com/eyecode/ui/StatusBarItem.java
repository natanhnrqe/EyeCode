package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StatusBarItem extends JPanel {

    private static final int MIN_HOVER_WIDTH = 28;

    private final JLabel label;
    private Runnable action;

    public StatusBarItem(String text) {
        this(text, null);
    }

    public StatusBarItem(Icon icon) {
        this(null, icon);
    }

    public StatusBarItem(String text, Icon icon) {
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.SM, 0, SpacingSystem.SM));

        label = new JLabel(text, icon, SwingConstants.LEADING);
        label.setFont(TypographyManager.UI_STATUS());
        label.setForeground(ColorManager.TEXT_SECONDARY);
        add(label);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                if (action != null) {
                    setOpaque(true);
                    setBackground(ColorManager.ACCENT_HOVER_BG);
                    repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (action != null) {
                    setOpaque(false);
                    repaint();
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (action != null) action.run();
            }
        });
    }

    public void setText(String text) {
        label.setText(text);
    }

    public String getText() {
        return label.getText();
    }

    public void setAction(Runnable action) {
        this.action = action;
        if (action != null) {
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMinimumSize(new Dimension(MIN_HOVER_WIDTH, 0));
        } else {
            setCursor(Cursor.getDefaultCursor());
        }
    }

    public void setForeground(Color color) {
        if (label != null) label.setForeground(color);
    }

    public void setIcon(Icon icon) {
        label.setIcon(icon);
    }
}
