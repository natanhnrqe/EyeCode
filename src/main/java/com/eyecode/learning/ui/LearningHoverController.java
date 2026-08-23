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
    private final Function<Integer, Optional<LearningConcept>> jdkConceptResolver;
    private final Function<Integer, Optional<LearningConcept>> memberConceptResolver;
    private final Function<String, Optional<LearningConcept>> syntaxConceptResolver;
    private final boolean ownsRenderer;
    private final IntConsumer moveListener;
    private final Runnable cancelListener;

    private volatile int lastOffset = -1;
    private volatile HoverSnapshot currentSnapshot;
    private volatile String visibleSymbolKey;
    private HoverSnapshot visibleSnapshot;
    private HoverSnapshot pendingSnapshot;

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
        this(surface, popup, scheduler, hoverEngine, syntaxSupplier, LearningRenderer::renderLesson, true,
                offset -> Optional.empty());
    }

    public LearningHoverController(
            LearningHoverSurface surface,
            LearningCardRenderer popup,
            LearningHoverScheduler scheduler,
            HoverEngine hoverEngine,
            Supplier<SyntaxSnapshot> syntaxSupplier,
            Function<String, String> contentLoader
    ) {
        this(surface, popup, scheduler, hoverEngine, syntaxSupplier, contentLoader, true,
                offset -> Optional.empty());
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
        this(surface, popup, scheduler, hoverEngine, syntaxSupplier, contentLoader, ownsRenderer,
                offset -> Optional.empty());
    }

    public LearningHoverController(
            LearningHoverSurface surface,
            LearningCardRenderer popup,
            LearningHoverScheduler scheduler,
            HoverEngine hoverEngine,
            Supplier<SyntaxSnapshot> syntaxSupplier,
            Function<String, String> contentLoader,
            boolean ownsRenderer,
            Function<Integer, Optional<LearningConcept>> jdkConceptResolver
    ) {
        this(surface, popup, scheduler, hoverEngine, syntaxSupplier, contentLoader, ownsRenderer,
                jdkConceptResolver, token -> Optional.empty(), new HoverStateMachine());
    }

    public LearningHoverController(
            LearningHoverSurface surface,
            LearningCardRenderer popup,
            LearningHoverScheduler scheduler,
            HoverEngine hoverEngine,
            Supplier<SyntaxSnapshot> syntaxSupplier,
            Function<String, String> contentLoader,
            boolean ownsRenderer,
            Function<Integer, Optional<LearningConcept>> jdkConceptResolver,
            Function<String, Optional<LearningConcept>> syntaxConceptResolver
    ) {
        this(surface, popup, scheduler, hoverEngine, syntaxSupplier, contentLoader, ownsRenderer,
                jdkConceptResolver, syntaxConceptResolver, offset -> Optional.empty(), new HoverStateMachine());
    }

    public LearningHoverController(
            LearningHoverSurface surface,
            LearningCardRenderer popup,
            LearningHoverScheduler scheduler,
            HoverEngine hoverEngine,
            Supplier<SyntaxSnapshot> syntaxSupplier,
            Function<String, String> contentLoader,
            boolean ownsRenderer,
            Function<Integer, Optional<LearningConcept>> jdkConceptResolver,
            Function<String, Optional<LearningConcept>> syntaxConceptResolver,
            Function<Integer, Optional<LearningConcept>> memberConceptResolver
    ) {
        this(surface, popup, scheduler, hoverEngine, syntaxSupplier, contentLoader, ownsRenderer,
                jdkConceptResolver, syntaxConceptResolver, memberConceptResolver, new HoverStateMachine());
    }

    LearningHoverController(
            LearningHoverSurface surface,
            LearningCardRenderer popup,
            LearningHoverScheduler scheduler,
            HoverEngine hoverEngine,
            Supplier<SyntaxSnapshot> syntaxSupplier,
            Function<String, String> contentLoader,
            boolean ownsRenderer,
            Function<Integer, Optional<LearningConcept>> jdkConceptResolver,
            Function<String, Optional<LearningConcept>> syntaxConceptResolver,
            HoverStateMachine stateMachine
    ) {
        this(surface, popup, scheduler, hoverEngine, syntaxSupplier, contentLoader, ownsRenderer,
                jdkConceptResolver, syntaxConceptResolver, offset -> Optional.empty(), stateMachine);
    }

    LearningHoverController(
            LearningHoverSurface surface,
            LearningCardRenderer popup,
            LearningHoverScheduler scheduler,
            HoverEngine hoverEngine,
            Supplier<SyntaxSnapshot> syntaxSupplier,
            Function<String, String> contentLoader,
            boolean ownsRenderer,
            Function<Integer, Optional<LearningConcept>> jdkConceptResolver,
            Function<String, Optional<LearningConcept>> syntaxConceptResolver,
            Function<Integer, Optional<LearningConcept>> memberConceptResolver,
            HoverStateMachine stateMachine
    ) {
        this.surface = surface;
        this.popup = popup;
        this.scheduler = scheduler;
        this.stateMachine = Objects.requireNonNull(stateMachine, "stateMachine");
        this.hoverEngine = hoverEngine;
        this.syntaxSupplier = syntaxSupplier;
        this.resolver = new DefaultLearningContextResolver();
        this.contentLoader = Objects.requireNonNull(contentLoader, "contentLoader");
        this.jdkConceptResolver = Objects.requireNonNull(jdkConceptResolver, "jdkConceptResolver");
        this.memberConceptResolver = Objects.requireNonNull(memberConceptResolver, "memberConceptResolver");
        this.syntaxConceptResolver = Objects.requireNonNull(syntaxConceptResolver, "syntaxConceptResolver");
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

            if (popup.isVisible()) {
                if (Objects.equals(visibleSymbolKey, snapshot.symbolKey())) {
                    cancelPendingSwitch();
                } else {
                    schedulePendingSwitch(snapshot);
                }
                return;
            }

            if (stateMachine.getState() == HoverState.WAITING) {
                scheduler.restartHover(this::tryShow);
                scheduler.startMonitor(this::monitorHover);
            }
            return;
        }

        cancelPendingSwitch();
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
        cancelPendingSwitch();
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
        if (insidePopup) {
            cancelPendingSwitch();
        }

        if (!insideEditor && !insidePopup) {
            cancelPendingSwitch();
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
            visibleSnapshot = null;
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
        visibleSnapshot = snapshot;
        popupShownAt = System.currentTimeMillis();

        loadLessonContent(snapshot);
    }

    private void schedulePendingSwitch(HoverSnapshot snapshot) {
        pendingSnapshot = snapshot;
        scheduler.restartHover(this::applyPendingSwitch);
        scheduler.startMonitor(this::monitorHover);
    }

    private void applyPendingSwitch() {
        HoverSnapshot pending = pendingSnapshot;
        if (pending == null || !popup.isVisible()
                || !Objects.equals(currentSnapshot, pending)
                || stateMachine.getState() == HoverState.INTERACTING) {
            return;
        }
        pendingSnapshot = null;
        popup.updateForExternalHover(pending.concept());
        visibleSymbolKey = pending.symbolKey();
        visibleSnapshot = pending;
        if (!Objects.equals(lastConcept, pending.concept())) {
            loadingContent = false;
            lastConcept = pending.concept();
            lastLessonPath = null;
            loadLessonContent(pending);
        }
    }

    private void cancelPendingSwitch() {
        pendingSnapshot = null;
        if (popup.isVisible()) {
            scheduler.stopHover();
        }
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
                        && t.type() == TokenType.IDENTIFIER)
                .findFirst();

        if (token.isPresent()) {
            SyntaxToken syntaxToken = token.get();
            Optional<LearningConcept> memberConcept = memberConceptResolver.apply(offset);
            if (memberConcept.isPresent()) {
                String key = "member:" + syntaxToken.startOffset() + ":" + syntaxToken.endOffset();
                if (Objects.equals(key, visibleSymbolKey) && popup.isVisible()) {
                    return visibleSnapshot;
                }
                return new HoverSnapshot(key, memberConcept.get());
            }
            Optional<LearningConcept> jdkConcept = jdkConceptResolver.apply(offset);
            if (jdkConcept.isPresent()) {
                String key = "jdk:" + syntaxToken.startOffset() + ":" + syntaxToken.endOffset();
                if (Objects.equals(key, visibleSymbolKey) && popup.isVisible()) {
                    return visibleSnapshot;
                }
                return new HoverSnapshot(key, jdkConcept.get());
            }
        }

        Optional<SyntaxToken> keyword = syntax.getTokens().stream()
                .filter(t -> offset >= t.startOffset() && offset <= t.endOffset()
                        && (t.type() == TokenType.KEYWORD
                        || t.type() == TokenType.IDENTIFIER))
                .findFirst();
        if (keyword.isEmpty()) {
            return null;
        }

        SyntaxToken syntaxToken = keyword.get();
        Optional<LearningConcept> syntaxConcept = syntaxConceptResolver.apply(syntaxToken.text());
        if (syntaxConcept.isPresent()) {
            String key = "syntax:" + syntaxToken.startOffset() + ":" + syntaxToken.endOffset();
            if (Objects.equals(key, visibleSymbolKey) && popup.isVisible()) {
                return visibleSnapshot;
            }
            return new HoverSnapshot(key, syntaxConcept.get());
        }
        if (syntaxToken.type() != TokenType.KEYWORD || !TYPE_KEYWORDS.contains(syntaxToken.text())) {
            return null;
        }
        String key = syntaxToken.text() + ":" + syntaxToken.startOffset() + ":" + syntaxToken.endOffset();

        if (Objects.equals(key, visibleSymbolKey) && popup.isVisible()) {
            return visibleSnapshot;
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
        visibleSnapshot = null;
        pendingSnapshot = null;
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
