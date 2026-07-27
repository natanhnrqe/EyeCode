package com.eyecode.learning.swing;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Component;

public final class SwingLearningFooter extends JPanel {

    private final JLabel updatedLabel;
    private final JLabel todayLabel;

    public SwingLearningFooter() {
        this("", "");
    }

    public SwingLearningFooter(String updated, String today) {
        super(new BorderLayout());
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(
                SwingLearningCardStyle.FOOTER_PADDING_TOP, 0,
                SwingLearningCardStyle.FOOTER_PADDING_BOTTOM, 0));

        JSeparator divider = new JSeparator(JSeparator.HORIZONTAL);
        divider.setForeground(SwingLearningCardStyle.FOOTER_DIVIDER_COLOR);
        divider.setBackground(SwingLearningCardStyle.FOOTER_DIVIDER_COLOR);
        add(divider, BorderLayout.NORTH);

        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        rowPanel.setOpaque(false);
        rowPanel.setBorder(BorderFactory.createEmptyBorder(
                SwingLearningCardStyle.FOOTER_TOP_GAP, 0, 0, 0));

        updatedLabel = new JLabel(updated != null ? updated : "");
        updatedLabel.setFont(SwingLearningCardStyle.footerFont());
        updatedLabel.setForeground(SwingLearningCardStyle.FOOTER_LABEL_COLOR);
        updatedLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        todayLabel = new JLabel(today != null ? today : "");
        todayLabel.setFont(SwingLearningCardStyle.footerFont());
        todayLabel.setForeground(SwingLearningCardStyle.FOOTER_VALUE_COLOR);
        todayLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        rowPanel.add(updatedLabel);
        rowPanel.add(javax.swing.Box.createHorizontalStrut(
                SwingLearningCardStyle.FOOTER_LABEL_GAP));
        rowPanel.add(todayLabel);
        rowPanel.add(javax.swing.Box.createHorizontalGlue());

        add(rowPanel, BorderLayout.CENTER);
    }

    public void setFooterText(String updated, String today) {
        if (updatedLabel != null) updatedLabel.setText(updated != null ? updated : "");
        if (todayLabel != null) todayLabel.setText(today != null ? today : "");
    }
}
