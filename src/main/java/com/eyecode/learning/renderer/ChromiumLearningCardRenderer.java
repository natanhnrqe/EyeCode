package com.eyecode.learning.renderer;

import com.eyecode.learning.diagnostic.LifecycleLogger;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.ui.LearningCard;
import com.eyecode.learning.ui.LearningHoverPopup;
import com.eyecode.ui.swing.SwingUIViewFactory;

import java.awt.Point;

public final class ChromiumLearningCardRenderer implements LearningCardRenderer {

    private final LearningHoverPopup popup;

    public ChromiumLearningCardRenderer() {
        this.popup = new LearningHoverPopup(new SwingUIViewFactory());
        this.popup.setCard(new LearningCard());
    }

    @Override
    public void show(LearningConcept concept) {
        LifecycleLogger.show(this, concept);
        popup.show(concept);
    }

    @Override
    public void hide() {
        LifecycleLogger.hide(this);
        popup.hide();
    }

    @Override
    public boolean isVisible() {
        return popup.isVisible();
    }

    @Override
    public void update(LearningConcept concept) {
        popup.update(concept);
    }

    @Override
    public void loadHtml(String html) {
        popup.loadHtml(html);
    }

    @Override
    public boolean containsScreen(Point screenPoint) {
        return popup.containsScreen(screenPoint);
    }

    @Override
    public void dispose() {
        LifecycleLogger.dispose(this);
        popup.hide();
    }

    public LearningHoverPopup getPopup() {
        return popup;
    }
}
