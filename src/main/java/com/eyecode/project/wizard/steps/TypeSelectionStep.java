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

        SwingUtilities.invokeLater(() -> printDiagnostics(panel));

        return panel;
    }

    private void printDiagnostics(JPanel root) {
        System.out.println();
        System.out.println("=== TypeSelectionStep Card Diagnostics ===");
        System.out.println("Root panel size: " + root.getWidth() + "x" + root.getHeight());
        System.out.println("Root panel layout: " + root.getLayout().getClass().getSimpleName());
        System.out.println("cardContainer size: " + cardContainer.getWidth() + "x" + cardContainer.getHeight());
        System.out.println("cardContainer layout: " + cardContainer.getLayout().getClass().getSimpleName());

        Component[] cards = cardContainer.getComponents();
        for (int i = 0; i < cards.length; i++) {
            JPanel card = (JPanel) cards[i];
            System.out.println();
            System.out.println("--- Card " + i + " ---");
            System.out.println("  Card size: " + card.getWidth() + "x" + card.getHeight());
            System.out.println("  Card prefSize: " + card.getPreferredSize().width + "x" + card.getPreferredSize().height);
            System.out.println("  Card layout: " + card.getLayout().getClass().getSimpleName());
            Insets ins = card.getInsets();
            System.out.println("  Card insets: top=" + ins.top + " left=" + ins.left
                + " bottom=" + ins.bottom + " right=" + ins.right);
            System.out.println("  Card border: " + (card.getBorder() != null ? card.getBorder().getClass().getSimpleName() : "null"));

            // X analysis: find minX, maxX, centerX used by BoxLayout
            int minX = Integer.MAX_VALUE;
            int maxRight = Integer.MIN_VALUE;
            for (Component child : card.getComponents()) {
                int r = child.getX() + child.getWidth();
                if (child.getX() < minX) minX = child.getX();
                if (r > maxRight) maxRight = r;
            }
            System.out.println("  X range used: leftmostX=" + minX + " rightmostRight=" + maxRight + " span=" + (maxRight - minX));
            System.out.println("  Center of card: " + (card.getWidth() / 2) + "  Center of used span: " + ((minX + maxRight) / 2));

            for (Component child : card.getComponents()) {
                Dimension pref = child.getPreferredSize();
                Dimension min  = child.getMinimumSize();
                Dimension max  = child.getMaximumSize();
                String name;
                if (child instanceof JLabel) {
                    String text = ((JLabel) child).getText();
                    name = child.getClass().getSimpleName()
                        + "[" + (text != null ? text.substring(0, Math.min(20, text.length())).replace("\n", "\\n") : "null") + "...]";
                } else {
                    name = child.getClass().getSimpleName();
                }
                System.out.println("  " + name);
                System.out.println("    bounds=(" + child.getX() + "," + child.getY() + ","
                    + child.getWidth() + "x" + child.getHeight() + ")"
                    + " pref=(" + pref.width + "x" + pref.height + ")"
                    + " min=(" + min.width + "x" + min.height + ")"
                    + " max=(" + max.width + "x" + max.height + ")"
                    + " alignX=" + child.getAlignmentX());

                // Check if X-edge is at 0 or right edge
                int rightEdge = child.getX() + child.getWidth();
                System.out.println("    rightEdge=" + rightEdge + " (cardWidth=" + card.getWidth() + ")");
                if (child.getX() == 0) System.out.println("    *** LEFT-EDGE ALIGNED ***");
                if (rightEdge == card.getWidth()) System.out.println("    *** RIGHT-EDGE ALIGNED ***");
                if (child.getAlignmentX() == 0.5f) {
                    int expectedCenterX = card.getWidth() / 2 - child.getWidth() / 2;
                    System.out.println("    CENTER_ALIGN expected x=" + expectedCenterX + " actual x=" + child.getX());
                }

                // Recursively print children of descriptionPanel
                if (child instanceof JPanel && ((JPanel) child).getLayout() instanceof BorderLayout) {
                    for (Component grandchild : ((JPanel) child).getComponents()) {
                        Dimension gpref = grandchild.getPreferredSize();
                        Dimension gmin  = grandchild.getMinimumSize();
                        Dimension gmax  = grandchild.getMaximumSize();
                        String gname;
                        if (grandchild instanceof JLabel) {
                            String gtext = ((JLabel) grandchild).getText();
                            gname = grandchild.getClass().getSimpleName()
                                + "[" + (gtext != null ? gtext.substring(0, Math.min(20, gtext.length())).replace("\n", "\\n") : "null") + "...]";
                        } else {
                            gname = grandchild.getClass().getSimpleName();
                        }
                        System.out.println("    └─ " + gname);
                        System.out.println("        bounds=(" + grandchild.getX() + "," + grandchild.getY() + ","
                            + grandchild.getWidth() + "x" + grandchild.getHeight() + ")"
                            + " pref=(" + gpref.width + "x" + gpref.height + ")"
                            + " min=(" + gmin.width + "x" + gmin.height + ")"
                            + " max=(" + gmax.width + "x" + gmax.height + ")"
                            + " alignX=" + grandchild.getAlignmentX());
                        int grightEdge = grandchild.getX() + grandchild.getWidth();
                        System.out.println("        rightEdge=" + grightEdge + " (panelWidth=" + child.getWidth() + ")");
                    }
                }
            }
        }
        System.out.println();
        System.out.println("=== End Diagnostics ===");
        System.out.println();
    }

    private JPanel createCard(ProjectTemplate t, int idx) {
        JPanel card = new JPanel() {
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
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(CARD_WIDTH, CARD_HEIGHT));

        int topPad = SpacingSystem.LG + SpacingSystem.XXS; // 12
        card.add(Box.createVerticalStrut(topPad));

        JLabel iconLabel = new JLabel(IconManager.projectTypeIcon(
                t.getType().getIconName() + "Project"), SwingConstants.CENTER);
        iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(iconLabel);

        card.add(Box.createVerticalStrut(SpacingSystem.XS)); // 4

        JLabel nameLabel = new JLabel(t.getDisplayName(), SwingConstants.CENTER);
        nameLabel.setFont(TypographyManager.UI_BODY());
        nameLabel.setForeground(ColorManager.TEXT_PRIMARY);
        nameLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(nameLabel);

        card.add(Box.createVerticalStrut(SpacingSystem.XXS)); // 2

        JLabel diffLabel = new JLabel(CARD_DIFFICULTY[idx][0], SwingConstants.CENTER);
        diffLabel.setFont(TypographyManager.UI_SMALL());
        diffLabel.setForeground(Color.decode(CARD_DIFFICULTY[idx][1]));
        diffLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(diffLabel);

        card.add(Box.createVerticalStrut(SpacingSystem.XS)); // 4

        JLabel descLabel = new JLabel(
                "<html><center>" + CARD_LONG_DESC[idx] + "</center></html>",
                SwingConstants.CENTER);
        descLabel.setFont(TypographyManager.UI_LABEL());
        descLabel.setForeground(ColorManager.TEXT_MUTED);

        JPanel descriptionPanel = new JPanel(new BorderLayout());
        descriptionPanel.setOpaque(false);
        descriptionPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        descriptionPanel.add(descLabel, BorderLayout.CENTER);
        card.add(descriptionPanel);

        card.add(Box.createVerticalGlue());

        JLabel footerLabel = new JLabel(
                "<html><center><i>" + CARD_FOOTER[idx] + "</i></center></html>",
                SwingConstants.CENTER);
        footerLabel.setFont(TypographyManager.UI_SMALL());
        footerLabel.setForeground(ColorManager.TEXT_TERTIARY);
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        card.add(footerLabel);

        card.add(Box.createVerticalStrut(topPad));

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
