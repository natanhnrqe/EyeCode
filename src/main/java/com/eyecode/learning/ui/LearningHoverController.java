package com.eyecode.learning.ui;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SymbolKind;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;
import com.eyecode.learning.analysis.DefaultLearningContextResolver;
import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.analysis.LearningContextResolver;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.hover.HoverEngine;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.LearningContext;
import com.eyecode.learning.render.LearningRenderer;
import com.eyecode.learning.renderer.LearningCardRenderer;

import java.awt.Point;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

public final class LearningHoverController {

    private static final Set<String> TYPE_KEYWORDS = Set.of("class", "interface", "enum", "record");

    private final LearningHoverSurface surface;
    private final LearningCardRenderer popup;
    private final LearningHoverScheduler scheduler;
    private final HoverStateMachine stateMachine;
    private final HoverEngine hoverEngine;
    private final Supplier<SyntaxSnapshot> syntaxSupplier;
    private final LearningContextResolver resolver;
    private final Function<String, String> contentLoader;
    private final boolean ownsRenderer;
    private final IntConsumer moveListener;
    private final Runnable cancelListener;

    private volatile int lastOffset = -1;
    private volatile HoverSnapshot currentSnapshot;
    private volatile String visibleSymbolKey;

    private boolean loadingContent;
    private LearningConcept lastConcept;
    private String lastLessonPath;
    private long popupShownAt = -1L;

    public LearningHoverController(
            LearningHoverSurface surface,
            LearningCardRenderer popup,
            LearningHoverScheduler scheduler,
            HoverEngine hoverEngine,
            Supplier<SyntaxSnapshot> syntaxSupplier
    ) {
        this(surface, popup, scheduler, hoverEngine, syntaxSupplier, LearningRenderer::renderLesson, true);
    }

    public LearningHoverController(
            LearningHoverSurface surface,
            LearningCardRenderer popup,
            LearningHoverScheduler scheduler,
            HoverEngine hoverEngine,
            Supplier<SyntaxSnapshot> syntaxSupplier,
            Function<String, String> contentLoader
    ) {
        this(surface, popup, scheduler, hoverEngine, syntaxSupplier, contentLoader, true);
    }

    public LearningHoverController(
            LearningHoverSurface surface,
            LearningCardRenderer popup,
            LearningHoverScheduler scheduler,
            HoverEngine hoverEngine,
            Supplier<SyntaxSnapshot> syntaxSupplier,
            Function<String, String> contentLoader,
            boolean ownsRenderer
    ) {
        this.surface = surface;
        this.popup = popup;
        this.scheduler = scheduler;
        this.stateMachine = new HoverStateMachine();
        this.hoverEngine = hoverEngine;
        this.syntaxSupplier = syntaxSupplier;
        this.resolver = new DefaultLearningContextResolver();
        this.contentLoader = Objects.requireNonNull(contentLoader, "contentLoader");
        this.ownsRenderer = ownsRenderer;
        this.moveListener = this::onOffsetChanged;
        this.cancelListener = this::cancelHover;

        this.surface.addMoveListener(moveListener);
        this.surface.addCancelListener(cancelListener);
    }

    public void dispose() {
        scheduler.dispose();
        popupShownAt = -1L;
        popup.hide();
        if (ownsRenderer) {
            popup.dispose();
        }
        surface.removeMoveListener(moveListener);
        surface.removeCancelListener(cancelListener);
        surface.dispose();
    }

    private void onOffsetChanged(int offset) {
        if (offset == lastOffset) {
            return;
        }

        lastOffset = offset;
        HoverSnapshot snapshot = resolveCurrentHover(offset);

        if (snapshot != null) {
            currentSnapshot = snapshot;
            stateMachine.enter(snapshot.symbolKey());

            if (stateMachine.getState() == HoverState.WAITING) {
                scheduler.restartHover(this::tryShow);
                scheduler.startMonitor(this::monitorHover);
            }

            if ((stateMachine.getState() == HoverState.VISIBLE
                    || stateMachine.getState() == HoverState.INTERACTING
                    || stateMachine.getState() == HoverState.HIDING)
                    && popup.isVisible()
                    && !Objects.equals(visibleSymbolKey, snapshot.symbolKey())) {
                popup.update(snapshot.concept());
                visibleSymbolKey = snapshot.symbolKey();
                if (!Objects.equals(lastConcept, snapshot.concept())) {
                    loadingContent = false;
                    lastConcept = snapshot.concept();
                    lastLessonPath = null;
                    loadLessonContent(snapshot);
                }
            }
            return;
        }

        if (withinGracePeriod()) {
            return;
        }

        stateMachine.leave();
        if (stateMachine.getState() == HoverState.IDLE) {
            scheduler.stopHover();
            scheduler.stopMonitor();
        }
    }

    private void cancelHover() {
        scheduler.stopHover();
        scheduler.stopMonitor();
        stateMachine.reset();
        popupShownAt = -1L;
        popup.hide();
        loadingContent = false;
        resetHover();
    }

    private boolean withinGracePeriod() {
        return popup.isVisible() && System.currentTimeMillis() - popupShownAt < 300L;
    }

    private void monitorHover() {
        Point mouse = surface.pointerScreenLocation();
        if (mouse == null) {
            return;
        }

        boolean insideEditor = surface.containsScreen(mouse);
        boolean insidePopup = popup.containsScreen(mouse);

        stateMachine.setPopupHover(insidePopup);

        if (!insideEditor && !insidePopup) {
            if (!withinGracePeriod()) {
                stateMachine.leave();
            }
            lastOffset = -1;
            if (stateMachine.getState() == HoverState.IDLE) {
                scheduler.stopHover();
                scheduler.stopMonitor();
            }
        }

        if (stateMachine.canHide()) {
            popupShownAt = -1L;
            popup.hide();
            loadingContent = false;
            visibleSymbolKey = null;
            currentSnapshot = null;
            lastOffset = -1;
            lastConcept = null;
            lastLessonPath = null;
            scheduler.stopHover();
            scheduler.stopMonitor();
            return;
        }

        if (stateMachine.getState() == HoverState.IDLE) {
            scheduler.stopHover();
            scheduler.stopMonitor();
        }
    }

    private void tryShow() {
        HoverDiagnosticLogger.log("controller.tryShow()");
        if (!stateMachine.canShow()) {
            return;
        }

        HoverSnapshot snapshot = currentSnapshot;
        if (snapshot == null) {
            return;
        }

        if (popup.isVisible()) {
            return;
        }

        loadingContent = false;
        lastConcept = snapshot.concept();
        lastLessonPath = null;

        popup.show(snapshot.concept());
        HoverDiagnosticLogger.logRendererShow();
        visibleSymbolKey = snapshot.symbolKey();
        popupShownAt = System.currentTimeMillis();

        loadLessonContent(snapshot);
    }

    private void loadLessonContent(HoverSnapshot snapshot) {
        if (loadingContent) {
            return;
        }

        LearningPage page = snapshot.concept().getPage();
        if (page == null) {
            return;
        }

        String resourcePath = page.getResourcePath();
        String contentIdentifier = page.getId() != null ? page.getId() : resourcePath;
        if (contentIdentifier != null && contentIdentifier.equals(lastLessonPath)) {
            return;
        }

        loadingContent = true;
        lastLessonPath = contentIdentifier;

        String html = contentLoader.apply(contentIdentifier);
        popup.loadHtml(html);
    }

    private HoverSnapshot resolveCurrentHover(int offset) {
        SyntaxSnapshot syntax = syntaxSupplier.get();
        if (syntax == null || syntax.isEmpty()) {
            return null;
        }

        Optional<SyntaxToken> token = syntax.getTokens().stream()
                .filter(t -> offset >= t.startOffset() && offset <= t.endOffset()
                        && t.type() == TokenType.KEYWORD
                        && TYPE_KEYWORDS.contains(t.text()))
                .findFirst();

        if (token.isEmpty()) {
            return null;
        }

        SyntaxToken syntaxToken = token.get();
        String key = syntaxToken.text() + ":" + syntaxToken.startOffset() + ":" + syntaxToken.endOffset();

        if (Objects.equals(key, visibleSymbolKey) && popup.isVisible()) {
            return currentSnapshot;
        }

        SymbolKind kind = keywordToKind(syntaxToken.text());
        if (kind == null) {
            return null;
        }

        LearningAnalysisContext analysisContext = resolveContext(kind, syntaxToken.text(), offset);
        if (analysisContext == null) {
            return null;
        }

        Optional<LearningConcept> concept = hoverEngine.resolve(analysisContext);
        if (concept.isEmpty()) {
            return null;
        }

        return new HoverSnapshot(key, concept.get());
    }

    private LearningAnalysisContext resolveContext(SymbolKind kind, String symbolName, int offset) {
        ProjectSymbol symbol = new ProjectSymbol();
        symbol.setKind(kind);
        symbol.setName(symbolName);

        LearningContext context = new LearningContext();
        context.setCurrentSymbol(symbol);
        context.setCursorOffset(offset);
        return resolver.resolve(context);
    }

    private void resetHover() {
        lastOffset = -1;
        currentSnapshot = null;
        visibleSymbolKey = null;
        lastConcept = null;
        lastLessonPath = null;
    }

    private static SymbolKind keywordToKind(String text) {
        return switch (text) {
            case "class" -> SymbolKind.CLASS;
            case "interface" -> SymbolKind.INTERFACE;
            case "enum" -> SymbolKind.ENUM;
            case "record" -> SymbolKind.RECORD;
            default -> null;
        };
    }

    private record HoverSnapshot(String symbolKey, LearningConcept concept) {
    }
}
