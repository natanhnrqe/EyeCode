package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

public final class SwingCodeBlock extends JPanel {

    private final JLabel headerLabel;
    private final JTextArea codeArea;

    public SwingCodeBlock(String language, String code) {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(ColorManager.CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER_CARD, 1),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)
        ));

        headerLabel = new JLabel(language != null ? language : "");
        headerLabel.setFont(LearningDocumentStyle.metaFont());
        headerLabel.setForeground(ColorManager.TEXT_TERTIARY);
        headerLabel.setBorder(new EmptyBorder(6, 10, 6, 10));
        headerLabel.setOpaque(true);
        headerLabel.setBackground(ColorManager.PANEL_BG);

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(true);
        header.setBackground(ColorManager.PANEL_BG);
        header.add(headerLabel, BorderLayout.WEST);

        codeArea = new JTextArea(code != null ? code : "");
        codeArea.setFont(LearningDocumentStyle.codeFont());
        codeArea.setForeground(ColorManager.EDITOR_FOREGROUND);
        codeArea.setBackground(ColorManager.EDITOR_BG);
        codeArea.setBorder(new EmptyBorder(12, 14, 12, 14));
        codeArea.setEditable(false);
        codeArea.setFocusable(false);
        codeArea.setLineWrap(false);
        codeArea.setWrapStyleWord(false);

        JPanel body = new JPanel(new BorderLayout());
        body.setOpaque(true);
        body.setBackground(ColorManager.EDITOR_BG);
        body.add(codeArea, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
    }

    public void setLanguage(String language) {
        headerLabel.setText(language != null ? language : "");
    }

    public void setCode(String code) {
        codeArea.setText(code != null ? code : "");
    }
}
