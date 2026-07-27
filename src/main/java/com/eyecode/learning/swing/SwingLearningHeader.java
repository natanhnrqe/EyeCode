package com.eyecode.learning.swing;

import com.eyecode.ui.designsystem.IconManager;

import javax.swing.BorderFactory;
import javax.swing.Icon;
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
        this(IconManager.javaFile(), "", "");
    }

    public SwingLearningHeader(Icon icon, String title, String subtitle) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(SwingLearningCardStyle.headerBorder());

        titleLabel = new JLabel(title != null ? title : "", icon, JLabel.LEFT);
        titleLabel.setFont(SwingLearningCardStyle.headerTitleFont());
        titleLabel.setForeground(SwingLearningCardStyle.HEADER_TITLE_COLOR);
        titleLabel.setIconTextGap(SwingLearningCardStyle.HEADER_ICON_TITLE_GAP);
        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitleLabel = new JLabel(subtitle != null ? subtitle : "");
        subtitleLabel.setFont(SwingLearningCardStyle.headerSubtitleFont());
        subtitleLabel.setForeground(SwingLearningCardStyle.HEADER_SUBTITLE_COLOR);
        subtitleLabel.setBorder(BorderFactory.createEmptyBorder(
                SwingLearningCardStyle.HEADER_SUBTITLE_TOP_GAP,
                SwingLearningCardStyle.HEADER_SUBTITLE_INDENT,
                0, 0));
        subtitleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        textPanel.add(titleLabel);
        textPanel.add(subtitleLabel);

        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(textPanel, BorderLayout.CENTER);

        JSeparator separator = new JSeparator(JSeparator.HORIZONTAL);
        separator.setForeground(SwingLearningCardStyle.HEADER_DIVIDER_COLOR);
        separator.setBackground(SwingLearningCardStyle.HEADER_DIVIDER_COLOR);

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
