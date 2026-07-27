package com.eyecode.learning.renderer;

import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardDocumentAdapter;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.RelatedConcept;
import com.eyecode.learning.service.DocumentationLearningCardActions;
import com.eyecode.learning.service.LearningDocumentationWindowService;
import com.eyecode.learning.ui.HoverDiagnosticLogger;
import com.eyecode.learning.swing.LearningCardActions;
import com.eyecode.learning.swing.SwingLearningCard;
import com.eyecode.ui.swing.SwingPopup;

import java.awt.Point;
import java.awt.Toolkit;
import java.util.List;

public final class SwingLearningCardRenderer implements LearningCardRenderer {

    private final SwingPopup popup;
    private final SwingLearningCard card;
    private final LearningDocumentationWindowService docService;
    private boolean visible;
    private LearningCardActions currentActions;

    public SwingLearningCardRenderer() {
        this.popup = new SwingPopup();
        this.card = new SwingLearningCard();
        this.docService = new LearningDocumentationWindowService();
        this.popup.setContent(card);
        this.popup.setFocusableWindowState(false);
        this.visible = false;
    }

    @Override
    public void show(LearningConcept concept) {
        HoverDiagnosticLogger.logRendererShow();
        renderConcept(concept);
        popup.getWindow().pack();
        positionPopup();
        popup.show();
        visible = true;
    }

    @Override
    public void update(LearningConcept concept) {
        if (!visible) {
            show(concept);
            return;
        }
        renderConcept(concept);
        popup.getWindow().pack();
        positionPopup();
    }

    private void renderConcept(LearningConcept concept) {
        LearningCardDocument document = LearningCardDocumentAdapter.fromConcept(concept);
        HoverDiagnosticLogger.logCardRender();
        List<RelatedConcept> related = LearningCardDocumentAdapter.relatedConceptsFrom(concept);
        this.currentActions = new DocumentationLearningCardActions(docService::open, concept, related);
        card.render(document);
        card.bindActions(currentActions, related);
    }

    private void positionPopup() {
        java.awt.PointerInfo pointerInfo = java.awt.MouseInfo.getPointerInfo();
        java.awt.Point mouse = pointerInfo != null ? pointerInfo.getLocation() : new java.awt.Point(200, 200);
        int x = mouse.x + 12;
        int y = mouse.y + 12;
        java.awt.Dimension size = popup.getWindow().getSize();
        java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        java.awt.Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration());
        if (x + size.width > screen.x + screen.width - insets.right) {
            x = mouse.x - 12 - size.width;
        }
        if (y + size.height > screen.y + screen.height - insets.bottom) {
            y = mouse.y - 12 - size.height;
        }
        popup.setLocation(x, y);
    }

    @Override
    public void hide() {
        popup.hide();
        visible = false;
    }

    @Override
    public boolean isVisible() {
        boolean result = visible && popup.isVisible();
        HoverDiagnosticLogger.logCardVisible(result);
        return result;
    }

    @Override
    public void loadHtml(String html) {
        // Not supported; ignored in Swing renderer
    }

    @Override
    public boolean containsScreen(Point screenPoint) {
        return visible && screenPoint != null && popup.getBounds().contains(screenPoint);
    }

    @Override
    public void dispose() {
        hide();
        card.clear();
        this.currentActions = null;
        this.visible = false;
    }
}
