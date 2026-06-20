package com.eyecode.project.wizard.steps;

import com.eyecode.project.ProjectType;
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
    private JPanel selectionPanel;
    private int selectedIndex = -1;

    private static final int CARD_ARC = 10;
    private static final int ACCENT_W = 3;
    private static final int CARD_WIDTH = 140;
    private static final int CARD_HEIGHT = 220;

    private static final Color CARD_SELECTED_BG = new Color(52, 55, 60);
    private static final Color CARD_NORMAL_BG = ColorManager.SURFACE_BG;
    private static final Color CARD_NORMAL_BORDER = new Color(55, 58, 64);
    private static final Color ACCENT_LINE = ColorManager.ACCENT_BLUE_LIGHT;

    private static final String[][] CARD_DIFFICULTY = {
        {"Beginner Friendly",       "#4EC9B0"},
        {"Intermediate",            "#E5C07B"},
        {"Intermediate",            "#E5C07B"},
        {"Intermediate / Advanced", "#E06C75"},
    };

    private static final String[] CARD_LONG_DESC = {
        "Build console applications and learn core programming concepts such as variables, loops, classes, and object-oriented design.",
        "Manage dependencies, project structure, and builds using the standard Java ecosystem tool.",
        "Modern build automation with flexible configuration and faster incremental builds.",
        "Create web applications, REST APIs, and enterprise systems using the most popular Java framework.",
    };

    private static final String[] CARD_FOOTER = {
        "Best for learning programming fundamentals.",
        "Recommended for most Java projects.",
        "Great for larger and scalable applications.",
        "Best for backend development.",
    };

    private static final String[][] CARD_INCLUDES = {
        {"Standard source layout", "Resources folder", "README documentation"},
        {"POM configuration", "Standard directory structure", "Dependency management"},
        {"Gradle DSL build", "Flexible configuration", "Incremental builds"},
        {"Auto-configuration", "Embedded server", "REST API support", "Maven structure"},
    };

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

        selectionPanel = new JPanel();
        selectionPanel.setOpaque(false);
        selectionPanel.setLayout(new BoxLayout(selectionPanel, BoxLayout.Y_AXIS));
        selectionPanel.setBorder(new EmptyBorder(SpacingSystem.LG + SpacingSystem.MD, 0, 0, 0));

        panel.add(heading, BorderLayout.NORTH);
        panel.add(cardContainer, BorderLayout.CENTER);
        panel.add(selectionPanel, BorderLayout.SOUTH);

        return panel;
    }

    private JPanel createCard(ProjectTemplate t, int idx) {
        JPanel card = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                int w = getWidth();
                int h = getHeight();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                boolean sel = (idx == selectedIndex);
                if (sel) {
                    g2.setColor(CARD_SELECTED_BG);
                    g2.fillRoundRect(ACCENT_W, 0, w - ACCENT_W, h, CARD_ARC, CARD_ARC);
                    g2.setColor(ACCENT_LINE);
                    g2.fillRoundRect(0, 2, ACCENT_W, h - 4, 3, 3);
                } else {
                    g2.setColor(CARD_NORMAL_BG);
                    g2.fillRoundRect(0, 0, w, h, CARD_ARC, CARD_ARC);
                    g2.setColor(CARD_NORMAL_BORDER);
                    g2.drawRoundRect(0, 0, w - 1, h - 1, CARD_ARC, CARD_ARC);
                }
                g2.dispose();
            }
        };
        card.setOpaque(false);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        card.setMinimumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));
        card.setMaximumSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));

        JLabel iconLabel = new JLabel(IconManager.projectTypeIcon(
                t.getType().getIconName() + "Project"), SwingConstants.CENTER);
        iconLabel.setBounds(0, SpacingSystem.LG + SpacingSystem.XXS,
                CARD_WIDTH, 28);

        JLabel nameLabel = new JLabel(t.getDisplayName(), SwingConstants.CENTER);
        nameLabel.setFont(TypographyManager.UI_BODY());
        nameLabel.setForeground(ColorManager.TEXT_PRIMARY);
        nameLabel.setBounds(0, SpacingSystem.LG + SpacingSystem.XXS + 32,
                CARD_WIDTH, 18);

        JLabel diffLabel = new JLabel(CARD_DIFFICULTY[idx][0], SwingConstants.CENTER);
        diffLabel.setFont(TypographyManager.UI_SMALL());
        diffLabel.setForeground(Color.decode(CARD_DIFFICULTY[idx][1]));
        diffLabel.setBounds(SpacingSystem.MD, SpacingSystem.LG + SpacingSystem.XXS + 52,
                CARD_WIDTH - SpacingSystem.MD * 2, 16);

        JLabel descLabel = new JLabel(
                "<html><center><small>" + CARD_LONG_DESC[idx] + "</small></center></html>",
                SwingConstants.CENTER);
        descLabel.setFont(TypographyManager.UI_SMALL());
        descLabel.setForeground(ColorManager.TEXT_MUTED);
        descLabel.setBounds(SpacingSystem.SM, SpacingSystem.LG + SpacingSystem.XXS + 72,
                CARD_WIDTH - SpacingSystem.SM * 2, 70);

        JLabel footerLabel = new JLabel(CARD_FOOTER[idx], SwingConstants.CENTER);
        footerLabel.setFont(TypographyManager.UI_SMALL().deriveFont(Font.ITALIC));
        footerLabel.setForeground(ColorManager.TEXT_TERTIARY);
        footerLabel.setBounds(SpacingSystem.XXS, CARD_HEIGHT - SpacingSystem.LG - SpacingSystem.XXS,
                CARD_WIDTH - SpacingSystem.XXS * 2, 14);

        card.add(iconLabel);
        card.add(nameLabel);
        card.add(diffLabel);
        card.add(descLabel);
        card.add(footerLabel);

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
        updateSelectionPanel();
    }

    private void updateSelectionPanel() {
        selectionPanel.removeAll();
        if (selectedIndex < 0 || selectedIndex >= templates.size()) return;

        ProjectTemplate t = templates.get(selectedIndex);
        int idx = selectedIndex;
        String nl = System.lineSeparator();

        JLabel title = new JLabel(t.getDisplayName());
        title.setFont(TypographyManager.UI_BODY().deriveFont(Font.BOLD, 14f));
        title.setForeground(ColorManager.TEXT_PRIMARY);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setBorder(new EmptyBorder(0, 0, SpacingSystem.XXS, 0));

        JLabel desc = new JLabel(CARD_LONG_DESC[idx]);
        desc.setFont(TypographyManager.UI_SMALL());
        desc.setForeground(ColorManager.TEXT_MUTED);
        desc.setAlignmentX(Component.LEFT_ALIGNMENT);
        desc.setBorder(new EmptyBorder(0, 0, SpacingSystem.SM, 0));

        JLabel includesTitle = new JLabel("Includes:");
        includesTitle.setFont(TypographyManager.UI_SMALL().deriveFont(Font.BOLD));
        includesTitle.setForeground(ColorManager.TEXT_SECONDARY);
        includesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        includesTitle.setBorder(new EmptyBorder(0, 0, SpacingSystem.XXS, 0));

        JPanel includesPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, SpacingSystem.XL, 0));
        includesPanel.setOpaque(false);
        includesPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (String item : CARD_INCLUDES[idx]) {
            JLabel itemLabel = new JLabel("\u2713 " + item);
            itemLabel.setFont(TypographyManager.UI_SMALL());
            itemLabel.setForeground(ColorManager.TEXT_TERTIARY);
            includesPanel.add(itemLabel);
        }

        selectionPanel.add(title);
        selectionPanel.add(desc);
        selectionPanel.add(includesTitle);
        selectionPanel.add(includesPanel);

        selectionPanel.revalidate();
        selectionPanel.repaint();
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
