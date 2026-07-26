package com.eyecode.learning.service;

import com.eyecode.learning.browser.LearningChromiumCard;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.render.LearningRenderer;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;

import com.eyecode.learning.service.DocumentationLifecycleLogger;

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
        DocumentationLifecycleLogger.logOpen();
        if (concept == null || concept.getPage() == null) {
            return;
        }
        String resourcePath = concept.getPage().getResourcePath();
        if (resourcePath == null || resourcePath.isBlank()) {
            return;
        }
        String html = LearningRenderer.renderLesson(resourcePath);
        DocumentationLifecycleLogger.logWindowCreated();
        DocumentationLifecycleLogger.logCardCreated();
        this.open = true;
        window.setVisible(true);
        window.toFront();
        DocumentationLifecycleLogger.logWindowVisible(true);
        DocumentationLifecycleLogger.logWindowBounds(window.getWidth(), window.getHeight());
        window.revalidate();
        window.repaint();
        DocumentationLifecycleLogger.logWindowDisplayable();
        card.loadHtml(html);
        DocumentationLifecycleLogger.logBrowserCreated();
        DocumentationLifecycleLogger.logLoadRequested();
        DocumentationLifecycleLogger.logBrowserComponentDisplayable();
        DocumentationLifecycleLogger.logBrowserComponentVisible(true);
        DocumentationLifecycleLogger.logFirstPaint();
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
