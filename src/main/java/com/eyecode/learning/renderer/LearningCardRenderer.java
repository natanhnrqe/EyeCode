package com.eyecode.learning.renderer;

import com.eyecode.learning.model.LearningConcept;

import java.awt.Point;

public interface LearningCardRenderer {

    void show(LearningConcept concept);

    void hide();

    boolean isVisible();

    void update(LearningConcept concept);

    default void updateForExternalHover(LearningConcept concept) {
        update(concept);
    }

    void loadHtml(String html);

    boolean containsScreen(Point screenPoint);

    void dispose();
}
