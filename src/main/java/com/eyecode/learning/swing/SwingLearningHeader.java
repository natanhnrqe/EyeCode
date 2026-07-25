package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.IconManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Component;

public final class SwingLearningHeader extends JPanel {

    private final JLabel titleLabel;
    private final JLabel subtitleLabel;

    public SwingLearningHeader() {
        super(new BorderLayout());
        setOpaque(false);

        titleLabel = new JLabel("Class", IconManager.javaFile(), JLabel.LEFT);
        titleLabel.setFont(LearningDocumentStyle.titleFont());
        titleLabel.setForeground(LearningDocumentStyle.titleColor());
        titleLabel.setIconTextGap(LearningDocumentStyle.titleIconTextGap());
        titleLabel.setBorder(LearningDocumentStyle.emptyBorder());

        subtitleLabel = new JLabel("Class • Inheritance");
        subtitleLabel.setFont(LearningDocumentStyle.subtitleFont());
        subtitleLabel.setForeground(LearningDocumentStyle.subtitleColor());
        subtitleLabel.setBorder(LearningDocumentStyle.emptyBorder());

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(LearningDocumentStyle.headerBorder());
        content.add(titleLabel, BorderLayout.WEST);
        content.add(subtitleLabel, BorderLayout.EAST);

        JSeparator separator = new JSeparator();
        separator.setForeground(LearningDocumentStyle.dividerColor());

        add(content, BorderLayout.CENTER);
        add(separator, BorderLayout.SOUTH);
    }

    public void setTitle(String title) {
        titleLabel.setText(title != null ? title : "");
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle != null ? subtitle : "");
    }
}
