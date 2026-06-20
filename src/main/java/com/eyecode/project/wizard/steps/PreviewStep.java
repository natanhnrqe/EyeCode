package com.eyecode.project.wizard.steps;

import com.eyecode.project.template.ProjectTemplate;
import com.eyecode.project.wizard.WizardState;
import com.eyecode.project.wizard.WizardStep;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public class PreviewStep implements WizardStep {

    private JTextArea summaryArea;
    private JTextArea structureArea;
    private JTextArea insightArea;

    @Override
    public String getTitle() { return "Preview"; }

    @Override
    public JComponent getComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorManager.WINDOW_BG);
        panel.setBorder(new EmptyBorder(SpacingSystem.LG + SpacingSystem.XL,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG,
                SpacingSystem.LG,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG));

        JLabel heading = new JLabel("Review your project");
        heading.setFont(TypographyManager.UI_TITLE());
        heading.setForeground(ColorManager.TEXT_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, SpacingSystem.LG + SpacingSystem.MD, 0));

        JPanel content = new JPanel(new GridBagLayout());
        content.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;

        summaryArea = new JTextArea();
        summaryArea.setEditable(false);
        summaryArea.setFont(TypographyManager.UI_BODY());
        summaryArea.setBackground(ColorManager.INPUT_BG);
        summaryArea.setForeground(ColorManager.TEXT_PRIMARY);
        summaryArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER_CARD),
                new EmptyBorder(SpacingSystem.SM, SpacingSystem.MD,
                        SpacingSystem.SM, SpacingSystem.MD)));

        JLabel summaryTitle = new JLabel("Project Summary");
        summaryTitle.setFont(TypographyManager.UI_LABEL());
        summaryTitle.setForeground(ColorManager.TEXT_SECONDARY);
        summaryTitle.setBorder(new EmptyBorder(0, 0, SpacingSystem.XXS, 0));

        JPanel summaryPanel = new JPanel(new BorderLayout());
        summaryPanel.setOpaque(false);
        summaryPanel.add(summaryTitle, BorderLayout.NORTH);
        summaryPanel.add(summaryArea, BorderLayout.CENTER);

        structureArea = new JTextArea();
        structureArea.setEditable(false);
        structureArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        structureArea.setBackground(ColorManager.INPUT_BG);
        structureArea.setForeground(ColorManager.TEXT_PRIMARY);
        structureArea.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER_CARD),
                new EmptyBorder(SpacingSystem.SM, SpacingSystem.MD,
                        SpacingSystem.SM, SpacingSystem.MD)));

        JLabel structureTitle = new JLabel("Project Structure");
        structureTitle.setFont(TypographyManager.UI_LABEL());
        structureTitle.setForeground(ColorManager.TEXT_SECONDARY);
        structureTitle.setBorder(new EmptyBorder(0, 0, SpacingSystem.XXS, 0));

        JPanel structurePanel = new JPanel(new BorderLayout());
        structurePanel.setOpaque(false);
        structurePanel.add(structureTitle, BorderLayout.NORTH);
        structurePanel.add(structureArea, BorderLayout.CENTER);

        insightArea = new JTextArea();
        insightArea.setEditable(false);
        insightArea.setFont(TypographyManager.UI_SMALL());
        insightArea.setBackground(ColorManager.WINDOW_BG);
        insightArea.setForeground(ColorManager.TEXT_MUTED);
        insightArea.setLineWrap(true);
        insightArea.setWrapStyleWord(true);
        insightArea.setBorder(new EmptyBorder(SpacingSystem.SM, 0, 0, 0));

        c.gridx = 0; c.gridy = 0; c.weighty = 0;
        c.insets = new Insets(0, 0, SpacingSystem.MD, 0);
        content.add(summaryPanel, c);

        c.gridy = 1; c.weighty = 1;
        c.insets = new Insets(0, 0, SpacingSystem.MD, 0);
        content.add(structurePanel, c);

        c.gridy = 2; c.weighty = 0;
        c.insets = new Insets(0, 0, 0, 0);
        content.add(insightArea, c);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(content, BorderLayout.CENTER);

        return panel;
    }

    @Override
    public String validate(WizardState state) {
        return null;
    }

    @Override
    public void onEnter(WizardState state) {
        String nl = System.lineSeparator();
        ProjectTemplate t = state.getTemplate();

        StringBuilder summary = new StringBuilder();
        summary.append("Project Name:  ").append(state.getProjectName()).append(nl);
        summary.append("Project Type:  ").append(t.getDisplayName()).append(nl);
        summary.append("Location:      ").append(state.getLocation()).append(nl);
        summary.append("Package:       ").append(state.getPackageBase()).append(nl);
        summary.append("JDK:           ").append(state.getJdkVersion()).append(nl);
        summary.append("Git Init:      ").append(state.isInitGit() ? "Enabled" : "Disabled").append(nl);
        summary.append("README:        ").append(state.isGenerateReadme() ? "Enabled" : "Disabled");
        summaryArea.setText(summary.toString());
        summaryArea.setCaretPosition(0);

        String type = t.getType().name();
        StringBuilder structure = new StringBuilder();
        structure.append(state.getProjectName()).append("/").append(nl);
        structure.append("  src/").append(nl);
        structure.append("    main/").append(nl);
        structure.append("      java/").append(nl);
        if (!type.equals("JAVA")) {
            structure.append("      resources/").append(nl);
            structure.append("    test/").append(nl);
            structure.append("      java/").append(nl);
            if (type.equals("MAVEN") || type.equals("SPRING_BOOT")) {
                structure.append("  pom.xml").append(nl);
            }
            if (type.equals("GRADLE")) {
                structure.append("  build.gradle").append(nl);
            }
            if (type.equals("SPRING_BOOT")) {
                structure.append("  application.properties").append(nl);
            }
        } else {
            structure.append("  resources/").append(nl);
        }
        structure.append("  README.md").append(nl);
        if (state.isInitGit()) {
            structure.append("  .gitignore").append(nl);
        }
        structureArea.setText(structure.toString());
        structureArea.setCaretPosition(0);

        if (t != null) {
            insightArea.setText(t.getDescription());
            insightArea.setCaretPosition(0);
        }
    }
}
