package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class TabComponent extends JPanel {

    private static final int ACCENT_LINE_HEIGHT = 2;

    private final JLabel titleLabel;
    private final JLabel iconLabel;
    private final JLabel modifiedLabel;
    private final JButton closeButton;
    private boolean selected;
    private boolean hovered;
    private boolean modified;

    public TabComponent(String title, String filename, String tooltipPath, Runnable onClose) {
        setOpaque(false);
        setRequestFocusEnabled(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
        setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.MD, 0, SpacingSystem.SM));

        iconLabel = new JLabel(IconManager.forFile(filename));
        iconLabel.setVerticalAlignment(SwingConstants.CENTER);
        iconLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, SpacingSystem.XS));

        titleLabel = new JLabel(title);
        titleLabel.setForeground(ColorManager.TEXT_PRIMARY);
        titleLabel.setFont(TypographyManager.UI_TAB());
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, ACCENT_LINE_HEIGHT, 0));

        modifiedLabel = new JLabel(IconManager.modifiedDot());
        modifiedLabel.setVisible(false);
        modifiedLabel.setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.XS, ACCENT_LINE_HEIGHT, 0));

        closeButton = new JButton(IconManager.close());
        closeButton.setBorderPainted(false);
        closeButton.setContentAreaFilled(false);
        closeButton.setFocusPainted(false);
        closeButton.setForeground(ColorManager.TEXT_TAB_CLOSE);
        closeButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        closeButton.setPreferredSize(new Dimension(SpacingSystem.TAB_CLOSE_SIZE, SpacingSystem.TAB_CLOSE_SIZE));
        closeButton.setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.XXS, ACCENT_LINE_HEIGHT, 0));
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
            if (onClose != null) onClose.run();
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (closeButton.isVisible() && closeButton.getBounds().contains(e.getPoint())) {
                    return;
                }
                Container parent = getParent();
                while (parent != null && !(parent instanceof JTabbedPane)) {
                    parent = parent.getParent();
                }
                if (parent instanceof JTabbedPane tabbedPane) {
                    for (int i = 0; i < tabbedPane.getTabCount(); i++) {
                        if (tabbedPane.getTabComponentAt(i) == TabComponent.this) {
                            tabbedPane.setSelectedIndex(i);
                            break;
                        }
                    }
                }
            }
        });

        String tabTooltip = (tooltipPath != null) ? tooltipPath : title;
        setToolTipText(tabTooltip);
        titleLabel.setToolTipText(tabTooltip);
        iconLabel.setToolTipText(tabTooltip);
        modifiedLabel.setToolTipText(tabTooltip);

        add(iconLabel);
        add(titleLabel);
        add(modifiedLabel);
        add(closeButton);
    }

    public void setTitle(String title) {
        titleLabel.setText(title);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        repaint();
    }

    public void setModified(boolean modified) {
        this.modified = modified;
        modifiedLabel.setVisible(modified);
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        if (selected) {
            g2.setColor(ColorManager.SELECTED_TAB_BG);
            g2.fillRect(0, 0, w, h);
            g2.setColor(ColorManager.ACCENT_BLUE_LIGHT);
            g2.fillRect(0, h - ACCENT_LINE_HEIGHT, w, ACCENT_LINE_HEIGHT);
        } else if (hovered) {
            g2.setColor(ColorManager.ACCENT_HOVER_BG);
            g2.fillRoundRect(3, 1, w - 6, h - 2, 6, 6);
        }

        g2.dispose();
        super.paintComponent(g);
    }
}
