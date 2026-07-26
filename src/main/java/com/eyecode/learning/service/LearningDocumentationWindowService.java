package com.eyecode.learning.service;

import com.eyecode.learning.browser.LearningChromiumCard;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.render.LearningRenderer;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;

public final class LearningDocumentationWindowService {

    private final JFrame window;
    private final LearningChromiumCard card;
    private boolean open;

    public LearningDocumentationWindowService() {
        this.window = new JFrame("Documentation");
        this.window.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        this.window.setSize(800, 600);
        this.window.setLocationRelativeTo(null);
        this.window.setLayout(new BorderLayout());
        this.card = new LearningChromiumCard();
        this.window.add(card, BorderLayout.CENTER);
        this.open = false;
    }

    public void open(LearningConcept concept) {
        if (concept == null || concept.getPage() == null) {
            return;
        }
        String resourcePath = concept.getPage().getResourcePath();
        if (resourcePath == null || resourcePath.isBlank()) {
            return;
        }
        String html = LearningRenderer.renderLesson(resourcePath);
        card.loadHtml(html);
        this.open = true;
        window.setVisible(true);
        window.toFront();
    }

    public void close() {
        window.dispose();
        this.open = false;
        card.dispose();
    }

    public boolean isOpen() {
        return this.open && window.isVisible();
    }
}
