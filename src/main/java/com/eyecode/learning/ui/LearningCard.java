package com.eyecode.learning.ui;

import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.learning.document.LearningDocumentView;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.ui.core.UIContainer;
import com.eyecode.ui.core.UIViewFactory;
import com.eyecode.ui.swing.SwingContainer;
import com.eyecode.ui.swing.SwingUIViewFactory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public final class LearningCard {

    private final SwingContainer container;
    private final LearningHeader header;
    private final LearningDocumentView documentView;
    private final LearningFooter footer;

    public LearningCard() {
        this(new SwingUIViewFactory());
    }

    public LearningCard(UIViewFactory viewFactory) {
        container = (SwingContainer) viewFactory.createContainer();
        container.getPanel().setLayout(new BorderLayout());
        container.getPanel().setOpaque(false);

        container.setPaintDelegate(this::paintCard);

        header = new LearningHeader(viewFactory);
        documentView = new LearningDocumentView();
        footer = new LearningFooter();

        container.add(header, BorderLayout.NORTH);
        container.add(documentView, BorderLayout.CENTER);
        container.add(footer, BorderLayout.SOUTH);
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

    public Component getComponent() {
        return container.getComponent();
    }

    private void paintCard(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        int arc = LearningDocumentStyle.cardArc();
        int origin = LearningDocumentStyle.cardOrigin();
        int borderInset = LearningDocumentStyle.cardBorderInset();
        int width = container.getPanel().getWidth();
        int height = container.getPanel().getHeight();
        g2.setColor(LearningDocumentStyle.cardBackground());
        g2.fillRoundRect(origin, origin, width, height, arc, arc);
        g2.setColor(LearningDocumentStyle.cardBorderColor());
        g2.drawRoundRect(origin, origin, width - borderInset, height - borderInset, arc, arc);
        g2.dispose();
    }

    private static LearningPage pageFor(LearningConcept concept) {
        return concept.getPage();
    }
}
