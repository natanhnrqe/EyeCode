package com.eyecode.ui;

import com.eyecode.project.template.ProjectConfig;
import com.eyecode.project.template.ProjectTemplateService;
import com.eyecode.project.wizard.WizardState;
import com.eyecode.project.wizard.WizardStep;
import com.eyecode.project.wizard.steps.ConfigurationStep;
import com.eyecode.project.wizard.steps.PreviewStep;
import com.eyecode.project.wizard.steps.TypeSelectionStep;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class NewProjectWizard extends JDialog {

    private final ProjectTemplateService templateService;
    private final Consumer<File> onProjectCreated;
    private final WizardState state;
    private final List<WizardStep> steps;
    private final CardLayout cardLayout;
    private final JPanel cardPanel;
    private final JButton backBtn;
    private final JButton nextBtn;
    private final JButton createBtn;
    private final JLabel stepLabel;
    private int currentStep;

    private static final int DIALOG_WIDTH = 820;
    private static final int DIALOG_HEIGHT = 600;

    public NewProjectWizard(Window owner, ProjectTemplateService templateService, Consumer<File> onProjectCreated) {
        super(owner, "New Project", ModalityType.APPLICATION_MODAL);
        this.templateService = templateService;
        this.onProjectCreated = onProjectCreated;
        this.state = new WizardState();
        this.steps = new ArrayList<>();
        this.cardLayout = new CardLayout();
        this.cardPanel = new JPanel(cardLayout);
        this.currentStep = 0;

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(ColorManager.WINDOW_BG);

        steps.add(new TypeSelectionStep(templateService));
        steps.add(new ConfigurationStep());
        steps.add(new PreviewStep());

        backBtn = new JButton("< Back");
        nextBtn = new JButton("Next >");
        createBtn = new JButton("Create Project");
        stepLabel = new JLabel();

        buildUi();
        showStep(0);
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ColorManager.WINDOW_BG);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorManager.WINDOW_BG);
        header.setBorder(new EmptyBorder(SpacingSystem.XL + SpacingSystem.XXL,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG,
                0,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG));

        JLabel title = new JLabel("New Project");
        title.setFont(TypographyManager.UI_WELCOME_TITLE().deriveFont(26f));
        title.setForeground(ColorManager.TEXT_PRIMARY);

        stepLabel.setFont(TypographyManager.UI_SMALL());
        stepLabel.setForeground(ColorManager.TEXT_TERTIARY);
        stepLabel.setBorder(new EmptyBorder(SpacingSystem.XXS, 0, 0, 0));

        header.add(title, BorderLayout.NORTH);
        header.add(stepLabel, BorderLayout.SOUTH);

        // Card area
        for (int i = 0; i < steps.size(); i++) {
            cardPanel.add(steps.get(i).getComponent(), "step_" + i);
        }

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(ColorManager.WINDOW_BG);
        footer.setBorder(new EmptyBorder(SpacingSystem.LG,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG,
                SpacingSystem.LG + SpacingSystem.XL,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG));

        styleButton(backBtn, false);
        styleButton(nextBtn, false);
        styleButton(createBtn, true);

        backBtn.addActionListener(e -> goBack());
        nextBtn.addActionListener(e -> goNext());
        createBtn.addActionListener(e -> onCreate());

        JPanel leftBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        leftBtns.setOpaque(false);
        leftBtns.add(backBtn);

        JPanel rightBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, SpacingSystem.SM, 0));
        rightBtns.setOpaque(false);
        rightBtns.add(nextBtn);
        rightBtns.add(createBtn);

        footer.add(leftBtns, BorderLayout.WEST);
        footer.add(rightBtns, BorderLayout.EAST);

        root.add(header, BorderLayout.NORTH);
        root.add(cardPanel, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        add(root);
    }

    private void showStep(int index) {
        currentStep = index;
        steps.get(index).onEnter(state);
        cardLayout.show(cardPanel, "step_" + index);

        stepLabel.setText("Step " + (index + 1) + " of " + steps.size() + " — " + steps.get(index).getTitle());

        backBtn.setVisible(index > 0);
        nextBtn.setVisible(index < steps.size() - 1);
        createBtn.setVisible(index == steps.size() - 1);
    }

    private void goBack() {
        if (currentStep > 0) {
            showStep(currentStep - 1);
        }
    }

    private void goNext() {
        String error = steps.get(currentStep).validate(state);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (currentStep < steps.size() - 1) {
            showStep(currentStep + 1);
        }
    }

    private void onCreate() {
        String error = steps.get(currentStep).validate(state);
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Validation", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Run all validations
        for (WizardStep step : steps) {
            String err = step.validate(state);
            if (err != null) {
                JOptionPane.showMessageDialog(this, err, "Validation", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        ProjectConfig config = new ProjectConfig();
        config.setName(state.getProjectName());
        config.setLocation(new File(state.getLocation()));
        config.setPackageBase(state.getPackageBase());
        config.setTemplate(state.getTemplate());
        config.setJdkVersion(state.getJdkVersion());
        config.setDescription(state.getDescription());
        config.setInitGit(state.isInitGit());
        config.setGenerateReadme(state.isGenerateReadme());

        try {
            File projectDir = templateService.createProject(config);

            // Post-generation: git init
            if (state.isInitGit()) {
                initGitRepo(projectDir);
            }

            dispose();
            if (onProjectCreated != null) {
                onProjectCreated.accept(projectDir);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this,
                    "Failed to create project:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initGitRepo(File projectDir) {
        File gitignore = new File(projectDir, ".gitignore");
        try {
            Files.writeString(gitignore.toPath(), """
                    target/
                    out/
                    *.class
                    *.jar
                    *.log
                    .idea/
                    .DS_Store
                    build/
                    .gradle/
                    """);
        } catch (IOException e) {
            System.err.println("Failed to write .gitignore: " + e.getMessage());
        }
    }

    private void styleButton(JButton btn, boolean primary) {
        btn.setFont(TypographyManager.UI_BUTTON());
        btn.setFocusPainted(false);
        int padH = SpacingSystem.LG + SpacingSystem.XXL + SpacingSystem.XXS;
        if (primary) {
            btn.setForeground(Color.WHITE);
            btn.setBackground(ColorManager.ACCENT_BLUE_LIGHT);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ColorManager.ACCENT_BLUE_LIGHT),
                    new EmptyBorder(SpacingSystem.XXS + SpacingSystem.XXS, padH,
                            SpacingSystem.XXS + SpacingSystem.XXS, padH)));
        } else {
            btn.setForeground(ColorManager.TEXT_PRIMARY);
            btn.setBackground(ColorManager.SURFACE_BG);
            btn.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(ColorManager.BORDER_CARD),
                    new EmptyBorder(SpacingSystem.XXS + SpacingSystem.XXS,
                            SpacingSystem.LG, SpacingSystem.XXS + SpacingSystem.XXS,
                            SpacingSystem.LG)));
        }
    }
}
