package com.eyecode.ui;

import com.eyecode.project.ProjectInfo;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;
import java.util.List;
import java.util.function.Consumer;

public class WelcomePanel extends JPanel {

    private static final int COLUMN_WIDTH = 580;
    private static final int BUTTON_HEIGHT = 44;
    private static final int BUTTON_ARC = 10;
    private static final int CARD_ARC = 10;
    private static final int ROW_HEIGHT = 42;
    private static final int MAX_VISIBLE_ROWS = 6;

    private final JPanel cardBody;
    private final JScrollPane cardScroll;
    private final JLabel emptyHint;
    private Consumer<ProjectInfo> onOpenRecentProject;

    private static final Color
        CARD_BORDER_SUBTLE = new Color(55, 58, 64),
        BTN_PRIMARY_BG     = new Color(42, 88, 188),
        BTN_PRIMARY_BORDER = new Color(56, 118, 232),
        BTN_PRIMARY_HOVER  = new Color(56, 112, 220),
        BTN_PRIMARY_HOVER_BORDER = new Color(72, 134, 244),
        BTN_PRIMARY_PRESSED = new Color(34, 72, 160),
        BTN_PRIMARY_PRESSED_BORDER = new Color(46, 98, 204),
        BTN_SEC_BG       = ColorManager.CARD_BG,
        BTN_SEC_BORDER   = ColorManager.BORDER_CARD,
        BTN_SEC_HOVER    = ColorManager.ACCENT_HOVER_BG,
        BTN_SEC_HOVER_BORDER = new Color(75, 78, 84),
        BTN_SEC_PRESSED  = ColorManager.ACCENT_SELECTION,
        BTN_SEC_PRESSED_BORDER = ColorManager.ACCENT_SELECTION;

    public WelcomePanel(Runnable onOpenProject, Runnable onNewProject, Consumer<ProjectInfo> onOpenRecentProject) {
        this.onOpenRecentProject = onOpenRecentProject;

        setLayout(new GridBagLayout());
        setBackground(ColorManager.EDITOR_BG);

        JPanel centerColumn = new JPanel();
        centerColumn.setOpaque(false);
        centerColumn.setLayout(new BoxLayout(centerColumn, BoxLayout.Y_AXIS));
        centerColumn.setAlignmentX(Component.CENTER_ALIGNMENT);

        // ── Zone 1: Branding ─────────────────────────────────
        JLabel title = new JLabel("EyeCode IDE");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setFont(TypographyManager.UI_WELCOME_TITLE());
        title.setForeground(ColorManager.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Learn Programming.  Learn Architecture.  Build Real Projects.");
        subtitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        subtitle.setFont(TypographyManager.UI_WELCOME_SUB());
        subtitle.setForeground(ColorManager.TEXT_MUTED);

        // ── Zone 2: Primary Actions ──────────────────────────
        JPanel actions = new JPanel(new GridLayout(1, 3, SpacingSystem.XXL, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.CENTER_ALIGNMENT);
        actions.setMaximumSize(new Dimension(COLUMN_WIDTH, BUTTON_HEIGHT));

        JButton openProjectBtn = createStyledButton("Open Project",
                IconManager.welcomeIcon("folderOpen"),
                BTN_PRIMARY_BG, BTN_PRIMARY_BORDER,
                BTN_PRIMARY_HOVER, BTN_PRIMARY_HOVER_BORDER,
                BTN_PRIMARY_PRESSED, BTN_PRIMARY_PRESSED_BORDER,
                Color.WHITE);
        openProjectBtn.addActionListener(e -> onOpenProject.run());

        JButton newProjectBtn = createStyledButton("New Project",
                IconManager.welcomeIcon("newProject"),
                BTN_SEC_BG, BTN_SEC_BORDER,
                BTN_SEC_HOVER, BTN_SEC_HOVER_BORDER,
                BTN_SEC_PRESSED, BTN_SEC_PRESSED_BORDER,
                ColorManager.TEXT_PRIMARY);
        newProjectBtn.addActionListener(e -> onNewProject.run());

        JButton cloneBtn = createStyledButton("Clone Repo",
                IconManager.welcomeIcon("git"),
                BTN_SEC_BG, BTN_SEC_BORDER,
                BTN_SEC_HOVER, BTN_SEC_HOVER_BORDER,
                BTN_SEC_PRESSED, BTN_SEC_PRESSED_BORDER,
                ColorManager.TEXT_PRIMARY);
        cloneBtn.addActionListener(e ->
                JOptionPane.showMessageDialog(this,
                        "Clone repository feature coming soon.",
                        "Clone Repository", JOptionPane.INFORMATION_MESSAGE));

        actions.add(openProjectBtn);
        actions.add(newProjectBtn);
        actions.add(cloneBtn);

        // ── Zone 3: Recent Projects Card ─────────────────────
        JLabel cardTitle = new JLabel("Recent Projects");
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardTitle.setFont(TypographyManager.UI_TITLE());
        cardTitle.setForeground(ColorManager.TEXT_SECONDARY);

        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ColorManager.SURFACE_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), CARD_ARC, CARD_ARC);
                g2.setColor(CARD_BORDER_SUBTLE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, CARD_ARC, CARD_ARC);
                g2.dispose();
            }

            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), CARD_ARC, CARD_ARC));
                super.paintChildren(g2);
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.setMaximumSize(new Dimension(COLUMN_WIDTH, Short.MAX_VALUE));

        JPanel cardInner = new JPanel();
        cardInner.setOpaque(false);
        cardInner.setLayout(new BoxLayout(cardInner, BoxLayout.Y_AXIS));
        cardInner.setBorder(new EmptyBorder(SpacingSystem.XL, SpacingSystem.LG, SpacingSystem.XL, SpacingSystem.LG));

        cardBody = new JPanel();
        cardBody.setOpaque(false);
        cardBody.setLayout(new BoxLayout(cardBody, BoxLayout.Y_AXIS));

        emptyHint = new JLabel("No recent projects");
        emptyHint.setAlignmentX(Component.CENTER_ALIGNMENT);
        emptyHint.setFont(TypographyManager.UI_BODY());
        emptyHint.setForeground(ColorManager.TEXT_DISABLED);

        cardScroll = new JScrollPane(cardBody);
        cardScroll.setOpaque(false);
        cardScroll.getViewport().setOpaque(false);
        cardScroll.setBorder(null);
        cardScroll.setMaximumSize(new Dimension(COLUMN_WIDTH - SpacingSystem.HUGE, ROW_HEIGHT * MAX_VISIBLE_ROWS + SpacingSystem.SM));

        cardInner.add(cardScroll);
        cardInner.add(emptyHint);
        card.add(cardInner, BorderLayout.CENTER);

        // ── Assemble column ──────────────────────────────────
        centerColumn.add(Box.createVerticalGlue());
        centerColumn.add(title);
        centerColumn.add(Box.createVerticalStrut(SpacingSystem.SM + SpacingSystem.XXS));
        centerColumn.add(subtitle);
        centerColumn.add(Box.createVerticalStrut(SpacingSystem.HUGE + SpacingSystem.XXXL + SpacingSystem.MD));
        centerColumn.add(actions);
        centerColumn.add(Box.createVerticalStrut(SpacingSystem.HUGE + SpacingSystem.XXXL + SpacingSystem.XXS));
        centerColumn.add(cardTitle);
        centerColumn.add(Box.createVerticalStrut(SpacingSystem.XL));
        centerColumn.add(card);
        centerColumn.add(Box.createVerticalGlue());

        add(centerColumn, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0,
                GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL,
                new Insets(0, 0, 0, 0), 0, 0));
        
    }

    public void setRecentProjects(List<ProjectInfo> projects) {
        cardBody.removeAll();
        boolean empty = (projects == null || projects.isEmpty());
        emptyHint.setVisible(empty);
        cardScroll.setVisible(!empty);

        if (!empty) {
            for (int i = 0; i < projects.size(); i++) {
                boolean last = (i == projects.size() - 1);
                cardBody.add(new ProjectRow(projects.get(i), last));
            }
            cardBody.add(Box.createVerticalGlue());
        }

        cardBody.revalidate();
        cardBody.repaint();
    }

    // ── Rounded button with custom painting ──────────────────
    private JButton createStyledButton(String text, Icon icon,
            Color normalBg, Color normalBorder,
            Color hoverBg, Color hoverBorder,
            Color pressedBg, Color pressedBorder,
            Color fg) {

        RoundedButton btn = new RoundedButton(text, icon, BUTTON_ARC, normalBg, normalBorder);
        btn.setContentAreaFilled(false);
        btn.setOpaque(false);
        btn.setFocusPainted(false);
        btn.setFont(TypographyManager.UI_BUTTON());
        btn.setForeground(fg);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setIconTextGap(SpacingSystem.XL);
        btn.setBorder(BorderFactory.createEmptyBorder(0, SpacingSystem.XXL + SpacingSystem.XXS, 0, SpacingSystem.XXL + SpacingSystem.XXS));

        btn.addChangeListener(e -> {
            ButtonModel m = btn.getModel();
            if (m.isPressed()) {
                btn.setVisual(pressedBg, pressedBorder);
            } else if (m.isRollover()) {
                btn.setVisual(hoverBg, hoverBorder);
            } else {
                btn.setVisual(normalBg, normalBorder);
            }
        });

        return btn;
    }

    // ── Custom button with rounded background + border ──────
    private static class RoundedButton extends JButton {
        private Color curBg;
        private Color curBorder;
        private final int arc;

        RoundedButton(String text, Icon icon, int arc, Color bg, Color border) {
            super(text, icon);
            this.arc = arc;
            this.curBg = bg;
            this.curBorder = border;
        }

        void setVisual(Color bg, Color border) {
            curBg = bg;
            curBorder = border;
            repaint();
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.height = Math.max(d.height, BUTTON_HEIGHT);
            return d;
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(curBg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
            g2.setColor(curBorder);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ── Recent project row with hover ────────────────────────
    private class ProjectRow extends JPanel {

        private final ProjectInfo projectInfo;
        private boolean hovered;

        ProjectRow(ProjectInfo info, boolean last) {
            this.projectInfo = info;
            setLayout(new BorderLayout());
            setOpaque(false);
            setMaximumSize(new Dimension(Short.MAX_VALUE, ROW_HEIGHT));
            setPreferredSize(new Dimension(0, ROW_HEIGHT));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

            if (!last) {
                setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorManager.BORDER_DIVIDER));
            } else {
                setBorder(new EmptyBorder(0, 0, 0, 0));
            }

            JLabel iconLabel = new JLabel(IconManager.welcomeIcon(info.getType().getIconName()));
            iconLabel.setBorder(new EmptyBorder(0, SpacingSystem.SM, 0, SpacingSystem.XL));

            JLabel nameLabel = new JLabel(info.getName());
            nameLabel.setFont(TypographyManager.UI_BODY());
            nameLabel.setForeground(ColorManager.TEXT_PRIMARY);

            JLabel typeLabel = new JLabel(info.getType().getDisplayName());
            typeLabel.setFont(TypographyManager.UI_SMALL());
            typeLabel.setForeground(ColorManager.TEXT_MUTED);
            typeLabel.setBorder(new EmptyBorder(0, SpacingSystem.XL, 0, SpacingSystem.XL));

            add(iconLabel, BorderLayout.WEST);
            add(nameLabel, BorderLayout.CENTER);
            add(typeLabel, BorderLayout.EAST);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) { setHovered(true); }
                @Override
                public void mouseExited(MouseEvent e)  { setHovered(false); }
                @Override
                public void mouseClicked(MouseEvent e) {
                    if (onOpenRecentProject != null) {
                        onOpenRecentProject.accept(projectInfo);
                    }
                }
            });
        }

        private void setHovered(boolean h) {
            if (hovered == h) return;
            hovered = h;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (hovered) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int arc = 8;
                g2.setColor(ColorManager.ACCENT_HOVER_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), arc, arc);
                g2.setColor(ColorManager.ACCENT_BLUE);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, arc, arc);
                g2.dispose();
            }
        }
    }
}
