package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.FlowLayout;

public final class SwingLearningFooter extends JPanel {

    public SwingLearningFooter() {
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

        JLabel updated = new JLabel("Updated:");
        updated.setFont(LearningDocumentStyle.metaFont());
        updated.setForeground(LearningDocumentStyle.subtitleColor());
        updated.setBorder(LearningDocumentStyle.emptyBorder());

        JLabel today = new JLabel("Today");
        today.setFont(LearningDocumentStyle.metaFont());
        today.setForeground(LearningDocumentStyle.subtitleColor());
        today.setBorder(LearningDocumentStyle.emptyBorder());

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.add(updated);
        textPanel.add(today);

        center.add(textPanel);
        add(center, BorderLayout.CENTER);
    }
}
