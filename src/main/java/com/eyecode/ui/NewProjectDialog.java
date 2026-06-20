package com.eyecode.ui;

import com.eyecode.project.template.ProjectConfig;
import com.eyecode.project.template.ProjectTemplate;
import com.eyecode.project.template.ProjectTemplateService;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

public class NewProjectDialog extends JDialog {

    private final ProjectTemplateService templateService;
    private final List<ProjectTemplate> templates;
    private final Consumer<File> onProjectCreated;

    private JPanel cardsPanel;
    private JTextField nameField;
    private JTextField locationField;
    private JTextField packageField;
    private JTextArea previewArea;
    private JTextArea descriptionArea;
    private ProjectTemplate selectedTemplate;
    private int selectedIndex;

    private static final int DIALOG_WIDTH = 820;
    private static final int DIALOG_HEIGHT = 580;
    private static final int LEFT_WIDTH = 220;
    private static final int CARD_ARC = 8;
    private static final int ACCENT_W = 3;

    private static final Color CARD_NORMAL_BG = ColorManager.SURFACE_BG;
    private static final Color CARD_SELECTED_BG = new Color(52, 55, 60);
    private static final Color CARD_NORMAL_BORDER = new Color(55, 58, 64);
    private static final Color CARD_SELECTED_BORDER = ColorManager.ACCENT_BLUE_LIGHT;
    private static final Color DIVIDER = ColorManager.BORDER;

    public NewProjectDialog(Window owner, ProjectTemplateService templateService, Consumer<File> onProjectCreated) {
        super(owner, "New Project", ModalityType.APPLICATION_MODAL);
        this.templateService = templateService;
        this.templates = templateService.getTemplates();
        this.onProjectCreated = onProjectCreated;
        this.selectedIndex = 0;

        setSize(DIALOG_WIDTH, DIALOG_HEIGHT);
        setResizable(false);
        setLocationRelativeTo(owner);
        getContentPane().setBackground(ColorManager.WINDOW_BG);

        buildUi();

        if (!templates.isEmpty()) {
            selectCard(0);
        }
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(ColorManager.WINDOW_BG);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildBody(), BorderLayout.CENTER);
        root.add(buildFooter(), BorderLayout.SOUTH);

        add(root);
    }

    // ── Header ───────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorManager.WINDOW_BG);
        header.setBorder(new EmptyBorder(SpacingSystem.XL + SpacingSystem.XXL,
                SpacingSystem.LG + SpacingSystem.XL,
                SpacingSystem.LG + SpacingSystem.XL,
                SpacingSystem.LG + SpacingSystem.XL));

        JLabel title = new JLabel("New Project");
        title.setFont(TypographyManager.UI_WELCOME_TITLE().deriveFont(28f));
        title.setForeground(ColorManager.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Create a new project and start learning software architecture.");
        subtitle.setFont(TypographyManager.UI_WELCOME_SUB());
        subtitle.setForeground(ColorManager.TEXT_TERTIARY);

        header.add(title, BorderLayout.NORTH);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }

    // ── Body: left cards + right config ──────────────────────
    private JPanel buildBody() {
        JPanel body = new JPanel(new BorderLayout());
        body.setBackground(ColorManager.WINDOW_BG);

        // Left panel with cards
        JPanel leftOuter = new JPanel(new BorderLayout());
        leftOuter.setBackground(ColorManager.WINDOW_BG);
        leftOuter.setPreferredSize(new Dimension(LEFT_WIDTH, 0));

        cardsPanel = new JPanel();
        cardsPanel.setLayout(new BoxLayout(cardsPanel, BoxLayout.Y_AXIS));
        cardsPanel.setBackground(ColorManager.WINDOW_BG);
        cardsPanel.setBorder(new EmptyBorder(SpacingSystem.XXS, SpacingSystem.SM, 0, SpacingSystem.SM));

        for (int i = 0; i < templates.size(); i++) {
            ProjectTemplate t = templates.get(i);
            TypeCard card = new TypeCard(t, i);
            cardsPanel.add(card);
            cardsPanel.add(Box.createVerticalStrut(SpacingSystem.XXS));
        }
        cardsPanel.add(Box.createVerticalGlue());

        leftOuter.add(cardsPanel, BorderLayout.NORTH);

        // Right panel
        JPanel right = new JPanel(new GridBagLayout());
        right.setBackground(ColorManager.WINDOW_BG);
        right.setBorder(new EmptyBorder(0,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG,
                SpacingSystem.LG,
                SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG));

        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.NORTHWEST;
        c.weightx = 1;

        // Config section label
        JLabel configTitle = new JLabel("Project Configuration");
        configTitle.setFont(TypographyManager.UI_TITLE());
        configTitle.setForeground(ColorManager.TEXT_PRIMARY);
        c.gridx = 0; c.gridy = 0; c.weighty = 0;
        c.insets = new Insets(0, 0, SpacingSystem.LG, 0);
        right.add(configTitle, c);

        // Fields
        JPanel fields = buildFields();
        c.gridy = 1; c.weighty = 0;
        c.insets = new Insets(0, 0, SpacingSystem.LG, 0);
        right.add(fields, c);

        // Preview section
        JPanel previewSection = buildPreviewSection();
        c.gridy = 2; c.weighty = 1;
        c.fill = GridBagConstraints.BOTH;
        c.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        right.add(previewSection, c);

        // Description
        descriptionArea = new JTextArea();
        descriptionArea.setEditable(false);
        descriptionArea.setFont(TypographyManager.UI_SMALL());
        descriptionArea.setBackground(ColorManager.WINDOW_BG);
        descriptionArea.setForeground(ColorManager.TEXT_MUTED);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setBorder(new EmptyBorder(SpacingSystem.SM, 0, 0, 0));
        c.gridy = 3; c.weighty = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 0, 0);
        right.add(descriptionArea, c);

        // Divider between left and right
        JSeparator divider = new JSeparator(JSeparator.VERTICAL);
        divider.setForeground(DIVIDER);

        body.add(leftOuter, BorderLayout.WEST);
        body.add(divider, BorderLayout.CENTER);
        body.add(right, BorderLayout.CENTER);

        return body;
    }

    private JPanel buildFields() {
        JPanel fields = new JPanel(new GridBagLayout());
        fields.setOpaque(false);

        nameField = createField();
        locationField = createField();
        packageField = createField();

        // Set default location
        locationField.setText(System.getProperty("user.home") + File.separator + "EyeCodeProjects");

        JButton browseBtn = new JButton("...");
        browseBtn.setFont(TypographyManager.UI_LABEL());
        browseBtn.setFocusPainted(false);
        browseBtn.setPreferredSize(new Dimension(32, 26));
        browseBtn.addActionListener(e -> browseLocation());

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, SpacingSystem.SM, 0);

        gbc.gridx = 0; gbc.gridy = 0;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, SpacingSystem.SM, SpacingSystem.SM);
        fields.add(fieldLabel("Name"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        fields.add(nameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, SpacingSystem.SM, SpacingSystem.SM);
        fields.add(fieldLabel("Location"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        fields.add(locationField, gbc);
        gbc.gridx = 2; gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, SpacingSystem.SM, 0);
        fields.add(browseBtn, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        gbc.weightx = 0;
        gbc.insets = new Insets(0, 0, 0, SpacingSystem.SM);
        fields.add(fieldLabel("Package"), gbc);
        gbc.gridx = 1; gbc.weightx = 1;
        gbc.insets = new Insets(0, 0, 0, 0);
        fields.add(packageField, gbc);

        return fields;
    }

    private JPanel buildPreviewSection() {
        previewArea = new JTextArea();
        previewArea.setEditable(false);
        previewArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
        previewArea.setBackground(ColorManager.INPUT_BG);
        previewArea.setForeground(ColorManager.TEXT_PRIMARY);
        previewArea.setBorder(new EmptyBorder(SpacingSystem.SM, SpacingSystem.MD,
                SpacingSystem.SM, SpacingSystem.MD));

        JScrollPane previewScroll = new JScrollPane(previewArea);
        previewScroll.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER_CARD),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));

        JLabel previewTitle = new JLabel("Project Structure");
        previewTitle.setFont(TypographyManager.UI_LABEL());
        previewTitle.setForeground(ColorManager.TEXT_SECONDARY);
        previewTitle.setBorder(new EmptyBorder(0, 0, SpacingSystem.XXS + SpacingSystem.XXS, 0));

        JPanel section = new JPanel(new BorderLayout());
        section.setOpaque(false);
        section.add(previewTitle, BorderLayout.NORTH);
        section.add(previewScroll, BorderLayout.CENTER);
        return section;
    }

    // ── Footer ───────────────────────────────────────────────
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(ColorManager.WINDOW_BG);
        footer.setBorder(new EmptyBorder(SpacingSystem.LG, SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG,
                SpacingSystem.LG + SpacingSystem.XL, SpacingSystem.LG + SpacingSystem.XL + SpacingSystem.LG));

        JButton cancelBtn = new JButton("Cancel");
        cancelBtn.setFont(TypographyManager.UI_BUTTON());
        cancelBtn.setFocusPainted(false);
        cancelBtn.setForeground(ColorManager.TEXT_PRIMARY);
        cancelBtn.setBackground(ColorManager.SURFACE_BG);
        cancelBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER_CARD),
                new EmptyBorder(SpacingSystem.XXS + SpacingSystem.XXS,
                        SpacingSystem.LG, SpacingSystem.XXS + SpacingSystem.XXS,
                        SpacingSystem.LG)));
        cancelBtn.addActionListener(e -> dispose());

        JButton createBtn = new JButton("Create Project");
        createBtn.setFont(TypographyManager.UI_BUTTON());
        createBtn.setFocusPainted(false);
        createBtn.setForeground(Color.WHITE);
        createBtn.setBackground(ColorManager.ACCENT_BLUE_LIGHT);
        createBtn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.ACCENT_BLUE_LIGHT),
                new EmptyBorder(SpacingSystem.XXS + SpacingSystem.XXS,
                        SpacingSystem.LG + SpacingSystem.XXL + SpacingSystem.XXS,
                        SpacingSystem.XXS + SpacingSystem.XXS,
                        SpacingSystem.LG + SpacingSystem.XXL + SpacingSystem.XXS)));
        createBtn.addActionListener(e -> onCreate());

        JPanel btnRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, SpacingSystem.SM, 0));
        btnRight.setOpaque(false);
        btnRight.add(createBtn);

        footer.add(cancelBtn, BorderLayout.WEST);
        footer.add(btnRight, BorderLayout.EAST);

        return footer;
    }

    // ── Card selection ───────────────────────────────────────
    private void selectCard(int index) {
        selectedIndex = index;
        selectedTemplate = templates.get(index);
        for (Component c : cardsPanel.getComponents()) {
            if (c instanceof TypeCard card) {
                card.setSelected(card.index == index);
            }
        }
        onTypeChanged();
    }

    private void onTypeChanged() {
        ProjectTemplate t = selectedTemplate;
        if (t == null) return;

        StringBuilder preview = new StringBuilder();
        for (String line : t.getStructurePreview()) {
            preview.append(line).append('\n');
        }
        previewArea.setText(preview.toString());
        previewArea.setCaretPosition(0);

        descriptionArea.setText(t.getDescription());
        descriptionArea.setCaretPosition(0);
    }

    // ── Type Card ────────────────────────────────────────────
    private class TypeCard extends JPanel {

        private final int index;
        private boolean selected;

        TypeCard(ProjectTemplate template, int idx) {
            this.index = idx;
            setLayout(new BorderLayout());
            setOpaque(false);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setMaximumSize(new Dimension(Short.MAX_VALUE, 54));
            setPreferredSize(new Dimension(0, 54));

            JLabel iconLabel = new JLabel(IconManager.projectTypeIcon(
                    template.getType().getIconName() + "Project"));
            iconLabel.setBorder(new EmptyBorder(0, SpacingSystem.MD, 0, SpacingSystem.SM));

            JPanel textPanel = new JPanel(new GridLayout(2, 1));
            textPanel.setOpaque(false);

            JLabel nameLabel = new JLabel(template.getDisplayName());
            nameLabel.setFont(TypographyManager.UI_BODY());
            nameLabel.setForeground(ColorManager.TEXT_PRIMARY);

            JLabel descLabel = new JLabel(template.getShortDescription());
            descLabel.setFont(TypographyManager.UI_SMALL());
            descLabel.setForeground(ColorManager.TEXT_MUTED);

            textPanel.add(nameLabel);
            textPanel.add(descLabel);

            add(iconLabel, BorderLayout.WEST);
            add(textPanel, BorderLayout.CENTER);

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    selectCard(index);
                }
            });
        }

        void setSelected(boolean sel) {
            if (this.selected == sel) return;
            this.selected = sel;
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth();
            int h = getHeight();
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (selected) {
                g2.setColor(CARD_SELECTED_BG);
                g2.fillRoundRect(ACCENT_W, 0, w - ACCENT_W, h, CARD_ARC, CARD_ARC);
                g2.setColor(CARD_SELECTED_BORDER);
                g2.fillRoundRect(0, 0, ACCENT_W, h, CARD_ARC, CARD_ARC);
                g2.fillRect(0, CARD_ARC, ACCENT_W, h - CARD_ARC * 2);
            } else {
                g2.setColor(CARD_NORMAL_BG);
                g2.fillRoundRect(0, 0, w, h, CARD_ARC, CARD_ARC);
                g2.setColor(CARD_NORMAL_BORDER);
                g2.drawRoundRect(0, 0, w - 1, h - 1, CARD_ARC, CARD_ARC);
            }

            g2.dispose();
        }
    }

    // ── Helpers ──────────────────────────────────────────────
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

    private void browseLocation() {
        JFileChooser chooser = new JFileChooser();
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Project Location");
        int result = chooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            locationField.setText(chooser.getSelectedFile().getAbsolutePath());
        }
    }

    private void onCreate() {
        ProjectConfig config = new ProjectConfig();
        config.setName(nameField.getText().trim());
        config.setLocation(new File(locationField.getText().trim()));
        config.setPackageBase(packageField.getText().trim());
        config.setTemplate(selectedTemplate);

        String error = config.validate();
        if (error != null) {
            JOptionPane.showMessageDialog(this, error, "Validation Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            File projectDir = templateService.createProject(config);
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
}
