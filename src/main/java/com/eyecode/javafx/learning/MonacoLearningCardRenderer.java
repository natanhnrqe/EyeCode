package com.eyecode.javafx.learning;

import com.eyecode.javafx.monaco.JavaFxMonacoEditorSurface;
import com.eyecode.javafx.monaco.MonacoOverlayAction;
import com.eyecode.javafx.monaco.MonacoOverlayEvent;
import com.eyecode.javafx.monaco.MonacoOverlayType;
import com.eyecode.language.documentation.JdkSourceResolver;
import com.eyecode.language.documentation.JavaJdkTypeCatalog;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.content.LearningDocument;
import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.renderer.LearningCardRenderer;

import java.awt.Point;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public final class MonacoLearningCardRenderer implements LearningCardRenderer, MonacoLearningOverlayPresenter {
    private static final String OVERLAY_ID = "learning";

    private final JavaFxMonacoEditorSurface surface;
    private final LearningContentEngine contentEngine;
    private final DocumentationNavigator documentationNavigator;
    private final SourceNavigator sourceNavigator;
    private final JdkSourceResolver sourceResolver = new JdkSourceResolver();
    private Consumer<Boolean> cardHoverListener = ignored -> { };
    private Runnable hiddenListener = () -> { };
    private boolean visible;
    private boolean disposed;
    private long generation;
    private int line = 1;
    private int column = 1;
    private String currentIdentifier;
    private LearningMetadata currentMetadata;
    private JdkSourceTarget currentSourceTarget;

    public MonacoLearningCardRenderer(JavaFxMonacoEditorSurface surface,
                                      LearningContentEngine contentEngine,
                                      DocumentationNavigator documentationNavigator,
                                      SourceNavigator sourceNavigator) {
        this.surface = surface;
        this.contentEngine = contentEngine;
        this.documentationNavigator = documentationNavigator == null ? target -> { } : documentationNavigator;
        this.sourceNavigator = sourceNavigator == null ? target -> { } : sourceNavigator;
    }

    public void setAnchor(int line, int column) {
        this.line = Math.max(1, line);
        this.column = Math.max(1, column);
    }

    @Override public void show(LearningConcept concept) {
        MonacoLearningContent content = prepare(concept);
        if (content != null) present(new MonacoLearningTarget("", 0, 0, 0, line, column, ""), content, visible);
    }

    @Override public void hide() {
        if (disposed) return;
        generation++;
        if (visible) surface.hideOverlay(OVERLAY_ID, generation);
        visible = false;
        hiddenListener.run();
    }

    @Override public void hardHide() {
        if (disposed) return;
        generation++;
        if (visible) surface.hardHideOverlay(OVERLAY_ID, generation);
        visible = false;
        hiddenListener.run();
    }

    @Override public void present(MonacoLearningTarget target, MonacoLearningContent content, boolean replaceVisible) {
        if (disposed || content == null) return;
        setAnchor(target.line(), target.column());
        currentIdentifier = content.concept().getPage() == null ? null : content.concept().getPage().getId();
        currentMetadata = content.metadata();
        currentSourceTarget = content.sourceTarget();
        generation++;
        if (visible) surface.updateOverlay(OVERLAY_ID, MonacoOverlayType.LEARNING,
                line, column, content.payload(), generation);
        else surface.showOverlay(OVERLAY_ID, MonacoOverlayType.LEARNING,
                line, column, content.payload(), generation);
        visible = true;
    }

    @Override public boolean isVisible() { return visible; }

    @Override public void update(LearningConcept concept) {
        show(concept);
    }

    @Override public void updateForExternalHover(LearningConcept concept) { update(concept); }

    @Override public void loadHtml(String html) {
        if (currentMetadata == null || disposed) return;
        publish(currentMetadata, ancestorsFor(currentMetadata), html, generation, false);
    }

    @Override public boolean containsScreen(Point screenPoint) { return false; }

    @Override public boolean supportsPointerOverCard() { return false; }

    @Override public void setCardHoverListener(Consumer<Boolean> listener) {
        cardHoverListener = listener == null ? ignored -> { } : listener;
    }

    @Override public void setPopupHiddenListener(Runnable listener) {
        hiddenListener = listener == null ? () -> { } : listener;
    }

    public void onOverlayEvent(MonacoOverlayEvent event) {
        if (event == null || disposed || !OVERLAY_ID.equals(event.overlayId())) return;
        if (event.type() == MonacoOverlayEvent.Type.POINTER_ENTER) {
            visible = true;
            cardHoverListener.accept(true);
        } else if (event.type() == MonacoOverlayEvent.Type.POINTER_LEAVE) {
            cardHoverListener.accept(false);
        } else if (event.type() == MonacoOverlayEvent.Type.HIDDEN) {
            visible = false;
            hiddenListener.run();
        } else if (event.type() == MonacoOverlayEvent.Type.ACTION) {
            if (event.generation() != generation) return;
            handleAction(event.action(), event.target());
        }
    }

    public void navigateToIdentifier(String identifier) {
        currentSourceTarget = null;
        LearningConcept concept = new LearningConcept();
        LearningPage page = new LearningPage(identifier);
        page.setId(identifier);
        concept.setPage(page);
        MonacoLearningContent content = prepare(concept);
        if (content != null) present(new MonacoLearningTarget("", 0, 0, 0, line, column, ""), content, true);
    }

    @Override public void dispose() {
        if (disposed) return;
        disposed = true;
        if (visible) surface.hardHideOverlay(OVERLAY_ID, ++generation);
        visible = false;
    }

    private void handleAction(MonacoOverlayAction action, String target) {
        if (action == MonacoOverlayAction.NAVIGATE_LEARNING && target != null && !target.isBlank()) {
            navigateToIdentifier(target);
        } else if (action == MonacoOverlayAction.OPEN_DOCS) {
            DocumentationTarget docs = documentationTarget(currentMetadata);
            if (docs != null) documentationNavigator.open(docs);
        } else if (action == MonacoOverlayAction.OPEN_SOURCE && currentSourceTarget != null) {
            sourceNavigator.open(currentSourceTarget);
        }
    }

    MonacoLearningContent prepare(LearningConcept concept) {
        if (concept == null || concept.getPage() == null || disposed) return null;
        String identifier = concept.getPage().getId();
        if (identifier == null || identifier.isBlank()) return null;
        LearningDocument document;
        try { document = contentEngine.loadDocument(identifier); }
        catch (RuntimeException ignored) { return null; }
        JdkSourceTarget sourceTarget = concept.getQualifiedName() == null ? null
                : JavaJdkTypeCatalog.findQualified(concept.getQualifiedName())
                        .flatMap(sourceResolver::resolve).orElse(null);
        if (document.metadata().sourceMember() != null && sourceTarget != null
                && sourceTarget.memberName() == null) {
            sourceTarget = sourceTarget.withMember(document.metadata().sourceMember());
        }
        List<LearningMetadata> related = relatedFor(document.metadata());
        DocumentationTarget docs = documentationTarget(document.metadata());
        boolean source = sourceTarget != null || sourceTarget(document.metadata()) != null;
        MonacoLearningOverlayPayload payload = MonacoLearningOverlayPayload.from(document.metadata(),
                ancestorsFor(document.metadata()), bodyHtml(document.renderedHtml()), related, docs, source);
        return new MonacoLearningContent(concept, document.metadata(), sourceTarget, payload.json());
    }

    private void publish(LearningMetadata metadata, List<LearningMetadata> ancestors,
                         String bodyHtml, long currentGeneration, boolean show) {
        List<LearningMetadata> related = relatedFor(metadata);
        DocumentationTarget docs = documentationTarget(metadata);
        boolean source = currentSourceTarget != null || sourceTarget(metadata) != null;
        MonacoLearningOverlayPayload payload = MonacoLearningOverlayPayload.from(metadata, ancestors,
                bodyHtml, related, docs, source);
        if (show && !visible) surface.showOverlay(OVERLAY_ID, MonacoOverlayType.LEARNING,
                line, column, payload.json(), currentGeneration);
        else surface.updateOverlay(OVERLAY_ID, MonacoOverlayType.LEARNING,
                line, column, payload.json(), currentGeneration);
    }

    private List<LearningMetadata> relatedFor(LearningMetadata metadata) {
        List<LearningMetadata> related = new ArrayList<>();
        for (String identifier : metadata.related()) {
            if (identifier.equals(metadata.parent())) continue;
            try { related.add(contentEngine.loadDocument(identifier).metadata()); }
            catch (RuntimeException ignored) { }
        }
        return related;
    }

    private DocumentationTarget documentationTarget(LearningMetadata metadata) {
        LearningMetadata reference = referenceMetadata(metadata);
        return reference == null ? null : reference.officialDocs();
    }

    private JdkSourceTarget sourceTarget(LearningMetadata metadata) {
        LearningMetadata reference = referenceMetadata(metadata);
        if (reference == null || reference.officialDocs() == null) return null;
        return JavaJdkTypeCatalog.findSimple(reference.officialDocs().label())
                .flatMap(sourceResolver::resolve)
                .map(target -> target.withMember(metadata.sourceMember())).orElse(null);
    }

    private LearningMetadata referenceMetadata(LearningMetadata metadata) {
        if (metadata == null || metadata.officialDocs() != null || metadata.parent() == null) return metadata;
        try { return contentEngine.loadDocument(metadata.parent()).metadata(); }
        catch (RuntimeException ignored) { return metadata; }
    }

    private List<LearningMetadata> ancestorsFor(LearningMetadata metadata) {
        List<LearningMetadata> ancestors = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        String parent = metadata.parent();
        while (parent != null && visited.add(parent) && ancestors.size() < 32) {
            try {
                LearningMetadata ancestor = contentEngine.loadDocument(parent).metadata();
                ancestors.add(0, ancestor);
                parent = ancestor.parent();
            } catch (RuntimeException ignored) { break; }
        }
        return ancestors;
    }

    private static String bodyHtml(String html) {
        if (html == null) return "";
        int start = html.indexOf("<body");
        start = start < 0 ? 0 : html.indexOf('>', start) + 1;
        int end = html.lastIndexOf("</body>");
        return end > start ? html.substring(start, end) : html;
    }
}
