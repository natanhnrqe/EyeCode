package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
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

        add(textPanel);
    }
}
