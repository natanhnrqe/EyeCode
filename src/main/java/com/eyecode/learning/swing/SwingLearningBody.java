package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.EmptyBorder;
import java.awt.Font;

public final class SwingLearningBody extends JScrollPane {

    private final JPanel contentPanel;

    public SwingLearningBody() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        // Fake content to validate spacing
        addTitle("Título");
        addParagraph("Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.");

        addTitle("Subtítulo");
        addParagraph("Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi.");

        addTitle("Código");
        SwingCodeBlock codeBlock = new SwingCodeBlock("Java", "public class Animal {\n\n    public void speak() {\n        System.out.println(\"Hello\");\n    }\n}\n");
        contentPanel.add(codeBlock);
        contentPanel.add(Box.createVerticalStrut(12));

        addTitle("Lista");
        addListItem("item 1");
        addListItem("item 2");

        contentPanel.add(Box.createVerticalGlue());

        setViewportView(contentPanel);
        setBorder(null);
        setOpaque(false);
        getViewport().setOpaque(false);
        getViewport().setBackground(ColorManager.CARD_BG);
    }

    private void addTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(LearningDocumentStyle.sectionTitleFont());
        label.setForeground(LearningDocumentStyle.titleColor());
        label.setBorder(new EmptyBorder(16, 0, 8, 0));
        contentPanel.add(label);
    }

    private void addParagraph(String text) {
        JTextArea area = new JTextArea(text);
        area.setFont(LearningDocumentStyle.bodyFont());
        area.setForeground(LearningDocumentStyle.bodyColor());
        area.setBackground(ColorManager.CARD_BG);
        area.setBorder(new EmptyBorder(4, 0, 16, 0));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setEditable(false);
        area.setOpaque(false);
        contentPanel.add(area);
    }

    private void addListItem(String text) {
        JLabel label = new JLabel("•  " + text);
        label.setFont(LearningDocumentStyle.bodyFont());
        label.setForeground(LearningDocumentStyle.bodyColor());
        label.setBorder(new EmptyBorder(2, 0, 2, 0));
        contentPanel.add(label);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
