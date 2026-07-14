package com.eyecode.learning.ui;

import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.learning.document.LearningDocumentView;
import com.eyecode.learning.model.LearningConcept;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class LearningCard extends JPanel {

    private final LearningHeader header;
    private final LearningDocumentView documentView;
    private final LearningFooter footer;

    public LearningCard() {
        super(new BorderLayout());
        setOpaque(false);

        header = new LearningHeader();
        documentView = new LearningDocumentView();
        footer = new LearningFooter();

        add(header, BorderLayout.NORTH);
        add(documentView, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);
    }

    public void setConcept(LearningConcept concept) {
        if (concept == null) {
            clear();
            return;
        }

        header.setConcept(concept);
        documentView.setPage(pageFor(concept));
        footer.setConcept(concept);
    }

    public void clear() {
        header.setConcept(null);
        documentView.clear();
        footer.setConcept(null);
    }

    public LearningDocumentView getDocumentView() {
        return documentView;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = LearningDocumentStyle.cardArc();
        int origin = LearningDocumentStyle.cardOrigin();
        int borderInset = LearningDocumentStyle.cardBorderInset();
        g2.setColor(LearningDocumentStyle.cardBackground());
        g2.fillRoundRect(origin, origin, getWidth(), getHeight(), arc, arc);
        g2.setColor(LearningDocumentStyle.cardBorderColor());
        g2.drawRoundRect(origin, origin, getWidth() - borderInset, getHeight() - borderInset, arc, arc);
        g2.dispose();
        super.paintComponent(g);
    }

    private static LearningPage pageFor(LearningConcept concept) {
        LearningPage page = concept.getPage();
        if (page == null || page.getSections() == null || page.getSections().isEmpty()) {
            return null;
        }
        return page;
    }
}
