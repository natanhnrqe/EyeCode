package com.eyecode.javafx.learning;

import com.eyecode.javafx.ui.toolwindow.content.JavaFxCeffxLearningSurface;
import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.content.LearningDocument;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.renderer.LearningCardRenderer;
import com.eyecode.language.documentation.JdkSourceResolver;
import com.eyecode.language.documentation.JavaJdkTypeCatalog;
import com.eyecode.language.documentation.JdkSourceTarget;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Popup;
import javafx.stage.Screen;
import javafx.stage.Window;
import javafx.geometry.Rectangle2D;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.scene.input.MouseEvent;

import java.awt.Point;
import java.awt.MouseInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class JavaFxLearningCardRenderer implements LearningCardRenderer {

    private static final int OFFSET = 14;

    private final JavaFxLearningAnchor anchor;
    private final JavaFxCeffxLearningSurface learningSurface;
    private final LearningContentEngine contentEngine;
    private final DocumentationNavigator documentationNavigator;
    private final SourceNavigator sourceNavigator;
    private final JdkSourceResolver sourceResolver = new JdkSourceResolver();
    private final JavaFxLearningCardHeader header = new JavaFxLearningCardHeader();
    private final JavaFxLearningCardFooter footer = new JavaFxLearningCardFooter();
    private final Popup popup = new Popup();
    private final VBox card;
    private boolean disposed;
    private boolean deferredShow;
    private boolean deferredShowAttempted;
    private long presentationGeneration;
    private String currentIdentifier;
    private JdkSourceTarget explicitSourceTarget;
    private Consumer<Boolean> cardHoverListener = ignored -> { };
    private Runnable popupHiddenListener = () -> { };
    private Window observedOwner;
    private final ChangeListener<Boolean> ownerVisibleListener = (observable, wasShowing, isShowing) -> {
        if (!isShowing) {
            hide();
        }
    };
    private final ChangeListener<Boolean> ownerFocusListener = (observable, wasFocused, isFocused) -> {
        if (!isFocused) {
            hide();
        }
    };

    public JavaFxLearningCardRenderer(
            JavaFxLearningAnchor anchor,
            JavaFxCeffxLearningSurface learningSurface,
            LearningContentEngine contentEngine,
            DocumentationNavigator documentationNavigator
    ) {
        this(anchor, learningSurface, contentEngine, documentationNavigator, target -> { });
    }

    public JavaFxLearningCardRenderer(
            JavaFxLearningAnchor anchor,
            JavaFxCeffxLearningSurface learningSurface,
            LearningContentEngine contentEngine,
            DocumentationNavigator documentationNavigator,
            SourceNavigator sourceNavigator
    ) {
        this.anchor = anchor;
        this.learningSurface = learningSurface;
        this.contentEngine = contentEngine;
        this.documentationNavigator = documentationNavigator == null ? target -> { } : documentationNavigator;
        this.sourceNavigator = sourceNavigator == null ? target -> { } : sourceNavigator;
        this.card = new VBox(header, learningSurface, footer);
        card.getStyleClass().add("learning-card");
        applySizing(null);
        learningSurface.getStyleClass().add("learning-card-body");
        VBox.setVgrow(learningSurface, Priority.ALWAYS);
        popup.setAutoHide(true);
        popup.setHideOnEscape(true);
        popup.setConsumeAutoHidingEvents(false);
        card.addEventFilter(MouseEvent.MOUSE_ENTERED, event -> cardHoverListener.accept(true));
        card.addEventFilter(MouseEvent.MOUSE_EXITED, event -> cardHoverListener.accept(false));
        popup.setOnHidden(event -> popupHiddenListener.run());
        popup.getContent().setAll(card);
    }

    @Override
    public void show(LearningConcept concept) {
        if (disposed) {
            return;
        }
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> show(concept));
            return;
        }
        explicitSourceTarget = concept == null || concept.getQualifiedName() == null
                ? null
                : JavaJdkTypeCatalog.findQualified(concept.getQualifiedName())
                        .flatMap(sourceResolver::resolve)
                        .orElse(null);
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
            deferShow(concept, presentationGeneration);
            return;
        }
        if (!owner.isShowing()) {
            deferShow(concept, presentationGeneration);
            return;
        }
        observeOwner(owner);
        deferredShowAttempted = false;
        popup.show(owner, point.x + OFFSET, point.y + OFFSET);
        positionWithinScreen(point.x + OFFSET, point.y + OFFSET);
    }

    @Override
    public void hide() {
        presentationGeneration++;
        popup.hide();
        deferredShowAttempted = false;
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
    }

    @Override
    public void updateForExternalHover(LearningConcept concept) {
        update(concept);
        if (concept != null && concept.getPage() != null) {
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
    public boolean isPointerOverCard() {
        if (!isVisible()) {
            return false;
        }
        try {
            var pointer = MouseInfo.getPointerInfo();
            return pointer != null && containsScreen(pointer.getLocation());
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    @Override
    public boolean supportsPointerOverCard() {
        return true;
    }

    @Override
    public void setCardHoverListener(Consumer<Boolean> listener) {
        cardHoverListener = listener == null ? ignored -> { } : listener;
    }

    @Override
    public void setPopupHiddenListener(Runnable listener) {
        popupHiddenListener = listener == null ? () -> { } : listener;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        detachOwner();
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
        return card.getPrefWidth();
    }

    double heightForTest() {
        return card.getPrefHeight();
    }

    VBox cardForTest() {
        return card;
    }

    public void navigateToIdentifier(String identifier) {
        explicitSourceTarget = null;
        showIdentifier(identifier);
    }

    public void setJdkSourceTarget(JdkSourceTarget target) {
        explicitSourceTarget = target;
        if (currentIdentifier != null) {
            String identifier = currentIdentifier;
            currentIdentifier = null;
            showIdentifier(identifier);
        }
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
        applySizing(document.metadata());
        header.show(document.metadata(), ancestorsFor(document.metadata()), this::navigate);
        footer.show(
                document.metadata(),
                this::navigate,
                documentationNavigator::open,
                this::navigate,
                this::titleFor,
                documentationTarget(document.metadata()),
                effectiveSourceTarget(document.metadata()),
                        sourceNavigator::open,
                        this::navigate
        );
        learningSurface.showHtml(document.renderedHtml());
    }

    private void deferShow(LearningConcept concept, long generation) {
        if (deferredShow || deferredShowAttempted || disposed) {
            return;
        }
        deferredShowAttempted = true;
        deferredShow = true;
        Platform.runLater(() -> {
            deferredShow = false;
            if (generation == presentationGeneration) {
                show(concept);
            }
        });
    }

    private void observeOwner(Window owner) {
        if (observedOwner == owner) {
            return;
        }
        detachOwner();
        observedOwner = owner;
        owner.showingProperty().addListener(ownerVisibleListener);
        owner.focusedProperty().addListener(ownerFocusListener);
    }

    private void detachOwner() {
        if (observedOwner == null) {
            return;
        }
        observedOwner.showingProperty().removeListener(ownerVisibleListener);
        observedOwner.focusedProperty().removeListener(ownerFocusListener);
        observedOwner = null;
    }

    private List<com.eyecode.learning.content.LearningMetadata> ancestorsFor(
            com.eyecode.learning.content.LearningMetadata metadata) {
        List<com.eyecode.learning.content.LearningMetadata> ancestors = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String parent = metadata.parent();
        while (parent != null && !parent.isBlank() && visited.add(parent)
                && ancestors.size() < 32) {
            try {
                com.eyecode.learning.content.LearningMetadata ancestor =
                        contentEngine.loadDocument(parent).metadata();
                ancestors.addFirst(ancestor);
                parent = ancestor.parent();
            } catch (RuntimeException ignored) {
                break;
            }
        }
        return ancestors;
    }

    private JdkSourceTarget sourceTarget(com.eyecode.learning.content.LearningMetadata metadata) {
        com.eyecode.learning.content.LearningMetadata targetMetadata = referenceMetadata(metadata);
        if (targetMetadata == null || targetMetadata.officialDocs() == null) {
            return null;
        }
        return JavaJdkTypeCatalog.findSimple(targetMetadata.officialDocs().label())
                .flatMap(sourceResolver::resolve)
                .map(target -> target.withMember(metadata.sourceMember()))
                .orElse(null);
    }

    private JdkSourceTarget effectiveSourceTarget(
            com.eyecode.learning.content.LearningMetadata metadata) {
        if (explicitSourceTarget == null) {
            return sourceTarget(metadata);
        }
        if (explicitSourceTarget.memberName() == null
                && metadata != null && metadata.sourceMember() != null) {
            return explicitSourceTarget.withMember(metadata.sourceMember());
        }
        return explicitSourceTarget;
    }

    private com.eyecode.learning.content.DocumentationTarget documentationTarget(
            com.eyecode.learning.content.LearningMetadata metadata) {
        com.eyecode.learning.content.LearningMetadata targetMetadata = referenceMetadata(metadata);
        return targetMetadata == null ? null : targetMetadata.officialDocs();
    }

    private com.eyecode.learning.content.LearningMetadata referenceMetadata(
            com.eyecode.learning.content.LearningMetadata metadata) {
        if (metadata == null || metadata.officialDocs() != null || metadata.parent() == null) {
            return metadata;
        }
        try {
            return contentEngine.loadDocument(metadata.parent()).metadata();
        } catch (RuntimeException ignored) {
            return metadata;
        }
    }

    private void navigate(String identifier) {
        explicitSourceTarget = null;
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
            positionWithinScreen(point.x + OFFSET, point.y + OFFSET);
        }
    }

    private void applySizing(com.eyecode.learning.content.LearningMetadata metadata) {
        LearningCardSizingPolicy sizing = LearningCardSizingPolicy.forMetadata(metadata);
        card.setMinSize(sizing.width(), sizing.minHeight());
        card.setPrefSize(sizing.width(), sizing.preferredHeight());
        card.setMaxSize(sizing.width(), sizing.maxHeight());
    }

    private void positionWithinScreen(double requestedX, double requestedY) {
        double width = card.getPrefWidth();
        double height = card.getPrefHeight();
        Rectangle2D bounds = Screen.getScreensForRectangle(
                        requestedX, requestedY, width, height)
                .stream()
                .findFirst()
                .map(Screen::getVisualBounds)
                .orElse(Screen.getPrimary().getVisualBounds());
        popup.setX(Math.max(bounds.getMinX(),
                Math.min(requestedX, bounds.getMaxX() - width)));
        popup.setY(Math.max(bounds.getMinY(),
                Math.min(requestedY, bounds.getMaxY() - height)));
    }
}
