package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class SwingLearningCard extends JPanel {

    private final SwingLearningHeader header;
    private final SwingLearningBody body;
    private final SwingLearningFooter footer;

    public SwingLearningCard() {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(LearningDocumentStyle.cardBackground());
        setBorder(BorderFactory.createLineBorder(
                LearningDocumentStyle.cardBorderColor(), 1));

        header = new SwingLearningHeader();
        body = new SwingLearningBody();
        footer = new SwingLearningFooter();

        add(header, BorderLayout.NORTH);
        add(body, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public SwingLearningHeader getHeader() {
        return header;
    }

    public SwingLearningBody getBody() {
        return body;
    }

    public SwingLearningFooter getFooter() {
        return footer;
    }
}
