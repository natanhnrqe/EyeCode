package com.eyecode.learning.renderer;

import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardDocumentAdapter;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.service.LearningDocumentationWindowService;
import com.eyecode.learning.swing.SwingLearningCard;
import com.eyecode.ui.swing.SwingPopup;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;

import java.awt.Point;

public final class SwingLearningCardRenderer implements LearningCardRenderer {

    private final SwingPopup popup;
    private final SwingLearningCard card;
    private final LearningDocumentationWindowService docService;
    private boolean visible;

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
        LearningCardDocument document = buildDocument(concept);
        card.getActionBar().setDocumentationAction(() -> {
            if (concept != null) {
                docService.open(concept);
            }
        });
        card.render(document);
        popup.show();
        visible = true;
    }

    private LearningCardDocument buildDocument(LearningConcept concept) {
        return LearningCardDocumentAdapter.fromConcept(concept);
    }

    @Override
    public void hide() {
        popup.hide();
        visible = false;
    }

    @Override
    public boolean isVisible() {
        return visible && popup.isVisible();
    }

    @Override
    public void update(LearningConcept concept) {
        show(concept);
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
        visible = false;
    }
}
