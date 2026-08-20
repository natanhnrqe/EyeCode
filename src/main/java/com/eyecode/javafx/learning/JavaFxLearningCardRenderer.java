package com.eyecode.javafx.learning;

import com.eyecode.javafx.ui.toolwindow.content.JavaFxCeffxLearningSurface;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.renderer.LearningCardRenderer;
import javafx.stage.Popup;
import javafx.stage.Window;

import java.awt.Point;

public final class JavaFxLearningCardRenderer implements LearningCardRenderer {

    private static final double WIDTH = 420;
    private static final double HEIGHT = 320;
    private static final int OFFSET = 14;

    private final JavaFxLearningHoverSurface hoverSurface;
    private final JavaFxCeffxLearningSurface learningSurface;
    private final Popup popup = new Popup();
    private boolean disposed;

    public JavaFxLearningCardRenderer(
            JavaFxLearningHoverSurface hoverSurface,
            JavaFxCeffxLearningSurface learningSurface
    ) {
        this.hoverSurface = hoverSurface;
        this.learningSurface = learningSurface;
        learningSurface.setMinSize(WIDTH, HEIGHT);
        learningSurface.setPrefSize(WIDTH, HEIGHT);
        learningSurface.setMaxSize(WIDTH, HEIGHT);
        learningSurface.getStyleClass().add("learning-hover-card");
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        popup.getContent().setAll(learningSurface);
    }

    @Override
    public void show(LearningConcept concept) {
        if (disposed || popup.isShowing()) {
            return;
        }
        Point point = hoverSurface.pointerScreenLocation();
        Window owner = hoverSurface.ownerWindow();
        if (point == null || owner == null) {
            return;
        }
        popup.show(owner, point.x + OFFSET, point.y + OFFSET);
    }

    @Override
    public void hide() {
        popup.hide();
    }

    @Override
    public boolean isVisible() {
        return popup.isShowing();
    }

    @Override
    public void update(LearningConcept concept) {
        if (isVisible()) {
            Point point = hoverSurface.pointerScreenLocation();
            if (point != null) {
                popup.setX(point.x + OFFSET);
                popup.setY(point.y + OFFSET);
            }
        }
    }

    @Override
    public void loadHtml(String html) {
        learningSurface.showHtml(html);
    }

    @Override
    public boolean containsScreen(Point screenPoint) {
        if (!isVisible() || screenPoint == null) {
            return false;
        }
        return screenPoint.x >= popup.getX()
                && screenPoint.x <= popup.getX() + popup.getWidth()
                && screenPoint.y >= popup.getY()
                && screenPoint.y <= popup.getY() + popup.getHeight();
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        popup.hide();
    }

}
