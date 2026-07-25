package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

public final class SwingLearningHeader extends JPanel {

    public SwingLearningHeader() {
        super(new BorderLayout());
        setOpaque(false);

        JLabel titleLabel = new JLabel("Class", IconManager.javaFile(), JLabel.LEFT);
        titleLabel.setFont(LearningDocumentStyle.titleFont());
        titleLabel.setForeground(LearningDocumentStyle.titleColor());
        titleLabel.setIconTextGap(LearningDocumentStyle.titleIconTextGap());
        titleLabel.setBorder(LearningDocumentStyle.emptyBorder());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("Inheritance");
        subtitleLabel.setFont(LearningDocumentStyle.subtitleFont());
        subtitleLabel.setForeground(LearningDocumentStyle.subtitleColor());
        subtitleLabel.setBorder(LearningDocumentStyle.emptyBorder());
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(titleLabel);
        textPanel.add(Box.createVerticalStrut(LearningDocumentStyle.headerTextGap()));
        textPanel.add(subtitleLabel);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(LearningDocumentStyle.headerBorder());
        content.add(textPanel, BorderLayout.CENTER);

        JSeparator separator = new JSeparator();
        separator.setForeground(LearningDocumentStyle.dividerColor());

        add(content, BorderLayout.CENTER);
        add(separator, BorderLayout.SOUTH);
    }
}
