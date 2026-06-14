package com.eyecode.ui;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
        content.setBorder(BorderFactory.createEmptyBorder(16, 16, 12, 16));
        content.setBackground(new Color(30, 30, 30));
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(0, 0, 8, 0);

        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        JLabel fontLabel = new JLabel("Font family");
        fontLabel.setForeground(new Color(187, 187, 187));
        fontLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        content.add(fontLabel, c);

        c.gridy = 1;
        fontFamily = new JComboBox<>(GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames());
        fontFamily.setSelectedItem(currentFont.getFamily());
        fontFamily.setBackground(new Color(45, 45, 45));
        fontFamily.setForeground(Color.WHITE);
        fontFamily.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        content.add(fontFamily, c);

        c.gridy = 2;
        c.insets = new Insets(12, 0, 8, 0);
        JLabel sizeLabel = new JLabel("Font size");
        sizeLabel.setForeground(new Color(187, 187, 187));
        sizeLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        content.add(sizeLabel, c);

        c.gridy = 3;
        c.insets = new Insets(0, 0, 12, 0);
        fontSize = new JSpinner(new SpinnerNumberModel(currentFont.getSize(), 8, 72, 1));
        fontSize.setBackground(new Color(45, 45, 45));
        fontSize.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        ((JSpinner.DefaultEditor) fontSize.getEditor()).getTextField().setBackground(new Color(45, 45, 45));
        ((JSpinner.DefaultEditor) fontSize.getEditor()).getTextField().setForeground(Color.WHITE);
        content.add(fontSize, c);

        c.gridy = 4;
        c.insets = new Insets(0, 0, 12, 0);
        preview = new JLabel("AaBbCc 0123 Code Sample");
        preview.setOpaque(true);
        preview.setBackground(new Color(25, 26, 28));
        preview.setForeground(new Color(188, 190, 196));
        preview.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(55, 58, 64)),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)
        ));
        preview.setHorizontalAlignment(SwingConstants.CENTER);
        updatePreview();
        content.add(preview, c);

        c.gridy = 5;
        c.insets = new Insets(0, 0, 0, 0);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);

        JButton cancelButton = new JButton("Cancel");
        cancelButton.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        cancelButton.setForeground(new Color(187, 187, 187));
        cancelButton.setBackground(new Color(58, 61, 67));
        cancelButton.setFocusPainted(false);
        cancelButton.addActionListener(e -> dispose());
        buttons.add(cancelButton);

        JButton applyButton = new JButton("Apply");
        applyButton.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        applyButton.setForeground(Color.WHITE);
        applyButton.setBackground(new Color(40, 94, 184));
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
