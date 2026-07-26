package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;

import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardHeaderData;
import com.eyecode.learning.model.LearningCardFooterData;

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
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(LearningDocumentStyle.cardBorderColor(), 1),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        header = new SwingLearningHeader();
        SwingLearningActionBar actionBar = new SwingLearningActionBar();
        body = new SwingLearningBody();
        footer = new SwingLearningFooter();

        JPanel centerArea = new JPanel(new BorderLayout());
        centerArea.setOpaque(false);
        centerArea.add(actionBar, BorderLayout.NORTH);
        centerArea.add(body, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(centerArea, BorderLayout.CENTER);
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

    public void render(LearningCardDocument document) {
        if (document == null) {
            clear();
            return;
        }
        if (document.getHeader() != null) {
            LearningCardHeaderData headerData = document.getHeader();
            header.setTitle(headerData.title());
            header.setSubtitle(headerData.subtitle());
            header.setIcon(headerData.iconKey() != null ? IconManager.javaFile() : null);
        } else {
            header.setTitle("");
            header.setSubtitle("");
        }

        body.setDocument(document);

        if (document.getFooter() != null) {
            LearningCardFooterData footerData = document.getFooter();
            footer.setFooterText(footerData.updatedLabel(), footerData.updatedValue());
        } else {
            footer.setFooterText("Updated:", "Today");
        }
    }

    public void clear() {
        header.setTitle("");
        header.setSubtitle("");
        body.clear();
        footer.setFooterText("Updated:", "Today");
    }
}
