package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public final class SwingLearningFooter extends JPanel {

    private final JLabel updatedLabel;
    private final JLabel todayLabel;

    public SwingLearningFooter() {
        this("Updated:", "Today");
    }

    public SwingLearningFooter(String updated, String today) {
        super(new FlowLayout(
                FlowLayout.CENTER,
                LearningDocumentStyle.footerHorizontalGap(),
                LearningDocumentStyle.footerVerticalGap()
        ));
        setOpaque(false);
        setBorder(LearningDocumentStyle.footerBorder());
        setLayout(new BorderLayout());

        JSeparator separator = new JSeparator();
        separator.setForeground(LearningDocumentStyle.dividerColor());
        add(separator, BorderLayout.NORTH);

        JPanel center = new JPanel(new FlowLayout(
                FlowLayout.CENTER,
                LearningDocumentStyle.footerHorizontalGap(),
                LearningDocumentStyle.footerVerticalGap()
        ));
        center.setOpaque(false);

        updatedLabel = new JLabel(updated != null ? updated : "");
        updatedLabel.setFont(LearningDocumentStyle.metaFont());
        updatedLabel.setForeground(LearningDocumentStyle.subtitleColor());
        updatedLabel.setBorder(LearningDocumentStyle.emptyBorder());

        todayLabel = new JLabel(today != null ? today : "");
        todayLabel.setFont(LearningDocumentStyle.metaFont());
        todayLabel.setForeground(LearningDocumentStyle.subtitleColor());
        todayLabel.setBorder(LearningDocumentStyle.emptyBorder());

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.X_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(updatedLabel);
        textPanel.add(Box.createHorizontalStrut(4));
        textPanel.add(todayLabel);

        center.add(textPanel);
        add(center, BorderLayout.CENTER);
    }

    public void setFooterText(String updated, String today) {
        if (updatedLabel != null) updatedLabel.setText(updated != null ? updated : "");
        if (todayLabel != null) todayLabel.setText(today != null ? today : "");
    }
}
