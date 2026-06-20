package com.eyecode.project.wizard.steps;

import com.eyecode.project.template.ProjectTemplate;
import com.eyecode.project.template.ProjectTemplateService;
import com.eyecode.project.wizard.WizardState;
import com.eyecode.project.wizard.WizardStep;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.List;

public class TypeSelectionStep implements WizardStep {

    private final ProjectTemplateService templateService;
    private final List<ProjectTemplate> templates;
    private JPanel cardContainer;
    private JTextArea insightArea;
    private int selectedIndex = -1;

    private static final int CARD_ARC = 10;
    private static final int ACCENT_W = 3;

    public TypeSelectionStep(ProjectTemplateService templateService) {
        this.templateService = templateService;
        this.templates = templateService.getTemplates();
    }

    @Override
    public String getTitle() { return "Project Type"; }

    @Override
    public JComponent getComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorManager.WINDOW_BG);
        panel.setBorder(new EmptyBorder(SpacingSystem.LG + SpacingSystem.XL,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG,
                SpacingSystem.LG,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG));

        JLabel heading = new JLabel("Choose a project type");
        heading.setFont(TypographyManager.UI_TITLE());
        heading.setForeground(ColorManager.TEXT_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, SpacingSystem.LG + SpacingSystem.MD, 0));

        cardContainer = new JPanel(new GridLayout(1, 4, SpacingSystem.LG, 0));
        cardContainer.setOpaque(false);

        for (int i = 0; i < templates.size(); i++) {
            ProjectTemplate t = templates.get(i);
            JPanel card = createCard(t, i);
            cardContainer.add(card);
        }

        insightArea = new JTextArea();
        insightArea.setEditable(false);
        insightArea.setFont(TypographyManager.UI_SMALL());
        insightArea.setBackground(ColorManager.WINDOW_BG);
        insightArea.setForeground(ColorManager.TEXT_MUTED);
        insightArea.setLineWrap(true);
        insightArea.setWrapStyleWord(true);
        insightArea.setBorder(new EmptyBorder(SpacingSystem.LG, 0, 0, 0));

        panel.add(heading, BorderLayout.NORTH);
        panel.add(cardContainer, BorderLayout.CENTER);
        panel.add(insightArea, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCard(ProjectTemplate t, int idx) {
        JPanel card = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                int w = getWidth();
                int h = getHeight();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean sel = (idx == selectedIndex);
                if (sel) {
                    g2.setColor(new Color(52, 55, 60));
                    g2.fillRoundRect(ACCENT_W, 0, w - ACCENT_W, h, CARD_ARC, CARD_ARC);
                    g2.setColor(ColorManager.ACCENT_BLUE_LIGHT);
                    g2.fillRoundRect(0, 2, ACCENT_W, h - 4, 3, 3);
                } else {
                    g2.setColor(ColorManager.SURFACE_BG);
                    g2.fillRoundRect(0, 0, w, h, CARD_ARC, CARD_ARC);
                    g2.setColor(new Color(55, 58, 64));
                    g2.drawRoundRect(0, 0, w - 1, h - 1, CARD_ARC, CARD_ARC);
                }
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(130, 120));

        JLabel iconLabel = new JLabel(IconManager.projectTypeIcon(
                t.getType().getIconName() + "Project"), SwingConstants.CENTER);
        JLabel nameLabel = new JLabel(t.getDisplayName(), SwingConstants.CENTER);
        nameLabel.setFont(TypographyManager.UI_BODY());
        nameLabel.setForeground(ColorManager.TEXT_PRIMARY);
        JLabel descLabel = new JLabel("<html><center>" + t.getShortDescription() + "</center></html>", SwingConstants.CENTER);
        descLabel.setFont(TypographyManager.UI_SMALL());
        descLabel.setForeground(ColorManager.TEXT_MUTED);

        JPanel center = new JPanel(new GridBagLayout());
        center.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0; c.gridwidth = 1;
        c.insets = new Insets(0, SpacingSystem.SM, 0, SpacingSystem.SM);

        c.gridy = 0; c.insets = new Insets(SpacingSystem.MD, SpacingSystem.SM, SpacingSystem.XXS, SpacingSystem.SM);
        center.add(iconLabel, c);
        c.gridy = 1; c.insets = new Insets(SpacingSystem.XXS, SpacingSystem.SM, 0, SpacingSystem.SM);
        center.add(nameLabel, c);
        c.gridy = 2; c.weighty = 1; c.insets = new Insets(SpacingSystem.XXS, SpacingSystem.SM, SpacingSystem.SM, SpacingSystem.SM);
        center.add(descLabel, c);

        card.add(center, BorderLayout.CENTER);

        final int fi = idx;
        card.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mousePressed(java.awt.event.MouseEvent e) {
                selectCard(fi);
            }
        });

        return card;
    }

    private void selectCard(int idx) {
        selectedIndex = idx;
        for (Component c : cardContainer.getComponents()) {
            if (c instanceof JPanel) c.repaint();
        }
        ProjectTemplate t = templates.get(idx);
        insightArea.setText(t.getDescription());
        insightArea.setCaretPosition(0);
    }

    @Override
    public String validate(WizardState state) {
        if (selectedIndex < 0 || selectedIndex >= templates.size())
            return "Please select a project type.";
        state.setTemplate(templates.get(selectedIndex));
        return null;
    }

    @Override
    public void onEnter(WizardState state) {
        if (state.getTemplate() != null) {
            for (int i = 0; i < templates.size(); i++) {
                if (templates.get(i).getType() == state.getTemplate().getType()) {
                    selectCard(i);
                    return;
                }
            }
        }
        if (selectedIndex < 0) {
            selectCard(0);
        }
    }
}
