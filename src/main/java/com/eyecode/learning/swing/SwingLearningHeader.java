package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;

public final class SwingLearningHeader extends JPanel {

    private final JLabel titleLabel;
    private final JLabel subtitleLabel;

    public SwingLearningHeader() {
        this(IconManager.javaFile(), "Class", "Class • Inheritance");
    }

    public SwingLearningHeader(Icon icon, String title, String subtitle) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(LearningDocumentStyle.headerBorder());

        // Icon + title horizontally
        titleLabel = new JLabel(title != null ? title : "", icon, JLabel.LEFT);
        titleLabel.setFont(TypographyManager.monoBold(15));
        titleLabel.setForeground(LearningDocumentStyle.titleColor());
        titleLabel.setIconTextGap(8);
        titleLabel.setBorder(LearningDocumentStyle.emptyBorder());
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Subtitle below, indented
        subtitleLabel = new JLabel(subtitle != null ? subtitle : "");
        subtitleLabel.setFont(TypographyManager.monoRegular(11));
        subtitleLabel.setForeground(LearningDocumentStyle.subtitleColor());
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(2, 22, 0, 0));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setBorder(LearningDocumentStyle.emptyBorder());
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(textPanel, BorderLayout.CENTER);

        JSeparator separator = new JSeparator();
        separator.setForeground(LearningDocumentStyle.dividerColor());

        add(content, BorderLayout.CENTER);
        add(separator, BorderLayout.SOUTH);
    }

    public void setIcon(Icon icon) {
        titleLabel.setIcon(icon);
    }

    public void setTitle(String title) {
        titleLabel.setText(title != null ? title : "");
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle != null ? subtitle : "");
    }

    public String title() {
        return titleLabel.getText();
    }

    public String subtitle() {
        return subtitleLabel.getText();
    }
}
