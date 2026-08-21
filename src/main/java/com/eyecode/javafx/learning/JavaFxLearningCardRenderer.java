package com.eyecode.javafx.learning;

import com.eyecode.javafx.ui.toolwindow.content.JavaFxCeffxLearningSurface;
import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.content.LearningDocument;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.renderer.LearningCardRenderer;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.geometry.Rectangle2D;

import java.awt.Point;

public final class JavaFxLearningCardRenderer implements LearningCardRenderer {

    private static final double WIDTH = 600;
    private static final double HEIGHT = 500;
    private static final int OFFSET = 14;

    private final JavaFxLearningAnchor anchor;
    private final JavaFxCeffxLearningSurface learningSurface;
    private final LearningContentEngine contentEngine;
    private final DocumentationNavigator documentationNavigator;
    private final JavaFxLearningCardHeader header = new JavaFxLearningCardHeader();
    private final JavaFxLearningCardFooter footer = new JavaFxLearningCardFooter();
    private final Popup popup = new Popup();
    private final VBox card;
    private boolean disposed;
    private String currentIdentifier;

    public JavaFxLearningCardRenderer(
            JavaFxLearningAnchor anchor,
            JavaFxCeffxLearningSurface learningSurface,
            LearningContentEngine contentEngine,
            DocumentationNavigator documentationNavigator
    ) {
        this.anchor = anchor;
        this.learningSurface = learningSurface;
        this.contentEngine = contentEngine;
        this.documentationNavigator = documentationNavigator == null ? target -> { } : documentationNavigator;
        this.card = new VBox(header, learningSurface, footer);
        card.getStyleClass().add("learning-card");
        card.setMinSize(WIDTH, HEIGHT);
        card.setPrefSize(WIDTH, HEIGHT);
        card.setMaxSize(WIDTH, HEIGHT);
        learningSurface.getStyleClass().add("learning-card-body");
        VBox.setVgrow(learningSurface, Priority.ALWAYS);
        popup.setAutoHide(false);
        popup.setHideOnEscape(false);
        popup.getContent().setAll(card);
    }

    @Override
    public void show(LearningConcept concept) {
        if (disposed) {
            return;
        }
        if (concept != null && concept.getPage() != null) {
            showIdentifier(concept.getPage().getId());
        }
        if (popup.isShowing()) {
            reposition();
            return;
        }
        Point point = anchor.point();
        Window owner = anchor.window();
        if (point == null || owner == null) {
            return;
        }
        popup.show(owner, point.x + OFFSET, point.y + OFFSET);
        positionWithinScreen(point.x + OFFSET, point.y + OFFSET);
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
        if (concept != null && concept.getPage() != null) {
            showIdentifier(concept.getPage().getId());
        }
        if (isVisible()) {
            reposition();
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

    JavaFxLearningCardHeader headerForTest() {
        return header;
    }

    JavaFxLearningCardFooter footerForTest() {
        return footer;
    }

    String currentIdentifierForTest() {
        return currentIdentifier;
    }

    double widthForTest() {
        return WIDTH;
    }

    double heightForTest() {
        return HEIGHT;
    }

    VBox cardForTest() {
        return card;
    }

    public void navigateToIdentifier(String identifier) {
        showIdentifier(identifier);
    }

    private void showIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank() || identifier.equals(currentIdentifier)) {
            return;
        }
        LearningDocument document;
        try {
            document = contentEngine.loadDocument(identifier);
        } catch (RuntimeException ignored) {
            return;
        }
        currentIdentifier = identifier;
        header.show(document.metadata());
        footer.show(
                document.metadata(),
                this::navigate,
                documentationNavigator::open,
                this::navigate,
                this::titleFor
        );
        learningSurface.showHtml(document.renderedHtml());
    }

    private void navigate(String identifier) {
        showIdentifier(identifier);
    }

    private String titleFor(String identifier) {
        try {
            return contentEngine.loadDocument(identifier).metadata().title();
        } catch (RuntimeException ignored) {
            return identifier;
        }
    }

    private void reposition() {
        Point point = anchor.point();
        if (point != null) {
            popup.setX(point.x + OFFSET);
            popup.setY(point.y + OFFSET);
        }
    }

    private void positionWithinScreen(double requestedX, double requestedY) {
        Rectangle2D bounds = Screen.getScreensForRectangle(
                        requestedX, requestedY, WIDTH, HEIGHT)
                .stream()
                .findFirst()
                .map(Screen::getVisualBounds)
                .orElse(Screen.getPrimary().getVisualBounds());
        popup.setX(Math.max(bounds.getMinX(),
                Math.min(requestedX, bounds.getMaxX() - WIDTH)));
        popup.setY(Math.max(bounds.getMinY(),
                Math.min(requestedY, bounds.getMaxY() - HEIGHT)));
    }
}
