package com.eyecode.learning.renderer;

import com.eyecode.learning.model.LearningConcept;

import java.awt.Point;
import java.util.function.Consumer;

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

    default void setCardHoverListener(Consumer<Boolean> listener) {
    }

    default void setPopupHiddenListener(Runnable listener) {
    }

    default boolean isPointerOverCard() {
        return false;
    }

    default boolean supportsPointerOverCard() {
        return false;
    }

    void dispose();
}
