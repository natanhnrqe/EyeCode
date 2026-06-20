package com.eyecode.project.wizard.steps;

import com.eyecode.project.wizard.WizardState;
import com.eyecode.project.wizard.WizardStep;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;

public class ConfigurationStep implements WizardStep {

    private JTextField nameField;
    private JTextField locationField;
    private JTextField packageField;
    private JComboBox<String> jdkCombo;
    private JTextField descField;
    private JCheckBox gitCheck;
    private JCheckBox readmeCheck;
    private JLabel errorLabel;

    @Override
    public String getTitle() { return "Configuration"; }

    @Override
    public JComponent getComponent() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(ColorManager.WINDOW_BG);
        panel.setBorder(new EmptyBorder(SpacingSystem.LG + SpacingSystem.XL,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG,
                SpacingSystem.LG,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG));

        JLabel heading = new JLabel("Configure your project");
        heading.setFont(TypographyManager.UI_TITLE());
        heading.setForeground(ColorManager.TEXT_PRIMARY);
        heading.setBorder(new EmptyBorder(0, 0, SpacingSystem.LG + SpacingSystem.MD, 0));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, SpacingSystem.SM, 0);

        nameField = createField();
        locationField = createField();
        packageField = createField();
        descField = createField();

        locationField.setText(System.getProperty("user.home") + File.separator + "EyeCodeProjects");

        JButton browseBtn = new JButton("...");
        browseBtn.setFont(TypographyManager.UI_LABEL());
        browseBtn.setFocusPainted(false);
        browseBtn.setPreferredSize(new Dimension(32, 26));
        browseBtn.addActionListener(e -> browseLocation());

        jdkCombo = new JComboBox<>(new String[]{"17", "21"});
        jdkCombo.setFont(TypographyManager.UI_BODY());
        jdkCombo.setBackground(ColorManager.INPUT_BG);
        jdkCombo.setForeground(ColorManager.TEXT_PRIMARY);
        ((JComponent) jdkCombo.getRenderer()).setBorder(
                new EmptyBorder(SpacingSystem.XXS, SpacingSystem.SM, SpacingSystem.XXS, SpacingSystem.SM));

        gitCheck = new JCheckBox("Initialize Git Repository");
        gitCheck.setFont(TypographyManager.UI_BODY());
        gitCheck.setForeground(ColorManager.TEXT_PRIMARY);
        gitCheck.setOpaque(false);
        gitCheck.setSelected(true);

        readmeCheck = new JCheckBox("Generate README.md");
        readmeCheck.setFont(TypographyManager.UI_BODY());
        readmeCheck.setForeground(ColorManager.TEXT_PRIMARY);
        readmeCheck.setOpaque(false);
        readmeCheck.setSelected(true);

        errorLabel = new JLabel(" ");
        errorLabel.setFont(TypographyManager.UI_SMALL());
        errorLabel.setForeground(ColorManager.ERROR_RED);

        int row = 0;
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        c.insets = new Insets(0, 0, SpacingSystem.SM, SpacingSystem.SM);
        form.add(fieldLabel("Name"), c);
        c.gridx = 1; c.weightx = 1;
        c.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        form.add(nameField, c);

        row++;
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        c.insets = new Insets(0, 0, SpacingSystem.SM, SpacingSystem.SM);
        form.add(fieldLabel("Location"), c);
        c.gridx = 1; c.weightx = 1;
        c.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        form.add(locationField, c);
        c.gridx = 2; c.weightx = 0;
        c.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        form.add(browseBtn, c);

        row++;
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        c.insets = new Insets(0, 0, SpacingSystem.SM, SpacingSystem.SM);
        form.add(fieldLabel("Package"), c);
        c.gridx = 1; c.weightx = 1;
        c.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        form.add(packageField, c);

        row++;
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        c.insets = new Insets(0, 0, SpacingSystem.SM, SpacingSystem.SM);
        form.add(fieldLabel("Language"), c);
        c.gridx = 1; c.weightx = 1;
        c.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        form.add(new JLabel("Java"), c);

        row++;
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        c.insets = new Insets(0, 0, SpacingSystem.SM, SpacingSystem.SM);
        form.add(fieldLabel("JDK"), c);
        c.gridx = 1; c.weightx = 0;
        c.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        form.add(jdkCombo, c);

        row++;
        c.gridx = 0; c.gridy = row; c.weightx = 0;
        c.insets = new Insets(0, 0, SpacingSystem.SM, SpacingSystem.SM);
        form.add(fieldLabel("Description"), c);
        c.gridx = 1; c.weightx = 1;
        c.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        form.add(descField, c);

        row++;
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(SpacingSystem.MD, 0, SpacingSystem.XXS, 0);
        form.add(gitCheck, c);

        row++;
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(SpacingSystem.XXS, 0, SpacingSystem.LG, 0);
        form.add(readmeCheck, c);

        row++;
        c.gridx = 0; c.gridy = row; c.gridwidth = 2;
        c.weightx = 1;
        c.insets = new Insets(0, 0, 0, 0);
        form.add(errorLabel, c);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(form, BorderLayout.NORTH);

        panel.add(heading, BorderLayout.NORTH);
        panel.add(bottom, BorderLayout.CENTER);

        return panel;
    }

    @Override
    public String validate(WizardState state) {
        String name = nameField.getText().trim();
        String location = locationField.getText().trim();
        String pkg = packageField.getText().trim();

        if (name.isEmpty()) {
            errorLabel.setText("Project name is required.");
            return "Project name is required.";
        }
        if (name.contains(" ")) {
            errorLabel.setText("Project name must not contain spaces.");
            return "Project name must not contain spaces.";
        }
        if (location.isEmpty()) {
            errorLabel.setText("Project location is required.");
            return "Project location is required.";
        }
        File locFile = new File(location);
        if (!locFile.exists()) {
            errorLabel.setText("Location directory does not exist.");
            return "Location directory does not exist.";
        }
        if (pkg.isEmpty()) {
            errorLabel.setText("Package base is required.");
            return "Package base is required.";
        }
        if (!pkg.matches("[a-z][a-z0-9]*(\\.[a-z][a-z0-9]*)*")) {
            errorLabel.setText("Invalid package syntax. Use lowercase identifiers separated by dots.");
            return "Invalid package syntax.";
        }
        File projDir = new File(location, name);
        if (projDir.exists()) {
            errorLabel.setText("Project directory already exists: " + name);
            return "Project directory already exists.";
        }

        errorLabel.setText(" ");
        state.setProjectName(name);
        state.setLocation(location);
        state.setPackageBase(pkg);
        state.setJdkVersion((String) jdkCombo.getSelectedItem());
        state.setDescription(descField.getText().trim());
        state.setInitGit(gitCheck.isSelected());
        state.setGenerateReadme(readmeCheck.isSelected());
        return null;
    }

    @Override
    public void onEnter(WizardState state) {
        if (state.getProjectName() != null) nameField.setText(state.getProjectName());
        if (state.getLocation() != null) locationField.setText(state.getLocation());
        if (state.getPackageBase() != null) packageField.setText(state.getPackageBase());
        if (state.getJdkVersion() != null) jdkCombo.setSelectedItem(state.getJdkVersion());
        if (state.getDescription() != null) descField.setText(state.getDescription());
        gitCheck.setSelected(state.isInitGit());
        readmeCheck.setSelected(state.isGenerateReadme());
        errorLabel.setText(" ");
    }

    private void browseLocation() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Project Location");
        int result = chooser.showOpenDialog(null);
        if (result == JFileChooser.APPROVE_OPTION) {
            locationField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private JLabel fieldLabel(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TypographyManager.UI_LABEL());
        label.setForeground(ColorManager.TEXT_SECONDARY);
        return label;
    }

    private JTextField createField() {
        JTextField field = new JTextField();
        field.setFont(TypographyManager.UI_BODY());
        field.setBackground(ColorManager.INPUT_BG);
        field.setForeground(ColorManager.TEXT_PRIMARY);
        field.setCaretColor(ColorManager.EDITOR_CARET);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER_CARD),
                new EmptyBorder(SpacingSystem.XXS + SpacingSystem.XXS,
                        SpacingSystem.SM, SpacingSystem.XXS + SpacingSystem.XXS,
                        SpacingSystem.SM)));
        return field;
    }
}
