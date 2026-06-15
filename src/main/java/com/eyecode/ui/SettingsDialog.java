package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class SettingsDialog extends JDialog {

    private final JComboBox<String> fontFamily;
    private final JSpinner fontSize;
    private final JLabel preview;
    private final Consumer<Font> onApply;

    public SettingsDialog(Window owner, Font currentFont, Consumer<Font> onApply) {
        super(owner, "Settings", ModalityType.APPLICATION_MODAL);
        this.onApply = onApply;

        setLayout(new BorderLayout());
        setSize(420, 320);
        setLocationRelativeTo(owner);
        setResizable(false);

        JPanel content = new JPanel(new GridBagLayout());
        content.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XXL, SpacingSystem.XXL, SpacingSystem.XL, SpacingSystem.XXL));
        content.setBackground(ColorManager.PANEL_BG);
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, SpacingSystem.MD, 0);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        JLabel fontLabel = new JLabel("Font family");
        fontLabel.setForeground(ColorManager.TEXT_SECONDARY);
        fontLabel.setFont(TypographyManager.UI_LABEL());
        content.add(fontLabel, c);

        c.gridy = 1;
        fontFamily = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontFamily.setSelectedItem(currentFont.getFamily());
        fontFamily.setBackground(ColorManager.INPUT_BG);
        fontFamily.setForeground(Color.WHITE);
        fontFamily.setFont(TypographyManager.UI_LABEL());
        content.add(fontFamily, c);

        c.gridy = 2;
        c.insets = new Insets(SpacingSystem.XL, 0, SpacingSystem.MD, 0);
        JLabel sizeLabel = new JLabel("Font size");
        sizeLabel.setForeground(ColorManager.TEXT_SECONDARY);
        sizeLabel.setFont(TypographyManager.UI_LABEL());
        content.add(sizeLabel, c);

        c.gridy = 3;
        c.insets = new Insets(0, 0, SpacingSystem.XL, 0);
        fontSize = new JSpinner(new SpinnerNumberModel(currentFont.getSize(), 8, 72, 1));
        fontSize.setBackground(ColorManager.INPUT_BG);
        fontSize.setFont(TypographyManager.UI_LABEL());
        ((JSpinner.DefaultEditor) fontSize.getEditor()).getTextField().setBackground(ColorManager.INPUT_BG);
        ((JSpinner.DefaultEditor) fontSize.getEditor()).getTextField().setForeground(Color.WHITE);
        content.add(fontSize, c);

        c.gridy = 4;
        c.insets = new Insets(0, 0, SpacingSystem.XL, 0);
        preview = new JLabel("AaBbCc 0123 Code Sample");
        preview.setOpaque(true);
        preview.setBackground(ColorManager.EDITOR_BG);
        preview.setForeground(ColorManager.EDITOR_FOREGROUND);
        preview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER),
                BorderFactory.createEmptyBorder(SpacingSystem.LG, SpacingSystem.XL, SpacingSystem.LG, SpacingSystem.XL)
        ));
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        updatePreview();
        content.add(preview, c);

        c.gridy = 5;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, SpacingSystem.MD, 0));
        buttons.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(TypographyManager.UI_LABEL());
        cancelButton.setForeground(ColorManager.TEXT_SECONDARY);
        cancelButton.setBackground(ColorManager.ACCENT_HOVER_BG);
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dispose());
        buttons.add(cancelButton);

        JButton applyButton = new JButton("Apply");
        applyButton.setFont(TypographyManager.UI_LABEL());
        applyButton.setForeground(Color.WHITE);
        applyButton.setBackground(ColorManager.ACCENT_BLUE);
        applyButton.setFocusPainted(false);
        applyButton.addActionListener(e -> apply());
        buttons.add(applyButton);

        c.gridx = 0;
        c.gridwidth = 2;
        content.add(buttons, c);

        add(content, BorderLayout.CENTER);

        fontFamily.addActionListener(e -> updatePreview());
        fontSize.addChangeListener(e -> updatePreview());

        setVisible(true);
    }

    private void updatePreview() {
        String family = (String) fontFamily.getSelectedItem();
        int size = (Integer) fontSize.getValue();
        preview.setFont(new Font(family, Font.PLAIN, size));
    }

    private void apply() {
        String family = (String) fontFamily.getSelectedItem();
        int size = (Integer) fontSize.getValue();
        if (onApply != null) {
            onApply.accept(new Font(family, Font.PLAIN, size));
        }
        dispose();
    }
}
