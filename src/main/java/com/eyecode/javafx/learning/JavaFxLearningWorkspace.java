package com.eyecode.javafx.learning;

import com.eyecode.javafx.ui.toolwindow.content.JavaFxCeffxLearningSurface;
import com.eyecode.learning.catalog.DefaultLearningCatalog;
import com.eyecode.learning.catalog.JdkLearningConceptCatalog;
import com.eyecode.learning.catalog.JavaSyntaxLearningCatalog;
import com.eyecode.learning.catalog.JavaSyntaxLearningResolver;
import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.language.documentation.DocumentationAtCaretResolver;
import com.eyecode.language.semantic.JavaMemberTargetResolver;
import com.eyecode.learning.hover.ConceptHoverProvider;
import com.eyecode.learning.hover.DefaultHoverEngine;
import com.eyecode.learning.ui.LearningHoverController;
import com.eyecode.learning.ui.LearningHoverSurface;
import com.eyecode.learning.renderer.LearningCardRenderer;
import com.eyecode.learning.concepts.DefaultLearningConceptEngine;
import com.eyecode.learning.concepts.providers.ClassConceptProvider;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.TokenType;
import javafx.application.Platform;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.fxmisc.richtext.CodeArea;

public final class JavaFxLearningWorkspace {

    private final JavaFxLearningAnchor anchor = new JavaFxLearningAnchor();
    private final JavaFxCeffxLearningSurface learningSurface;
    private final LearningCardRenderer renderer;
    private final LearningContentEngine contentEngine;
    private final DocumentationNavigator documentationNavigator;
    private final JavaMemberTargetResolver memberTargetResolver = new JavaMemberTargetResolver();
    private final JdkLearningConceptCatalog jdkCatalog = new JdkLearningConceptCatalog();
    private final JavaSyntaxLearningCatalog syntaxCatalog = new JavaSyntaxLearningCatalog();
    private final JavaSyntaxLearningResolver syntaxResolver = new JavaSyntaxLearningResolver(syntaxCatalog);
    private final DocumentationAtCaretResolver jdkResolver = new DocumentationAtCaretResolver();
    private final ExecutorService monacoLearningExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "eyecode-learning-preparation");
        thread.setDaemon(true);
        return thread;
    });
    private boolean disposed;

    public JavaFxLearningWorkspace() {
        this(target -> { });
    }

    public JavaFxLearningWorkspace(DocumentationNavigator documentationNavigator) {
        this(documentationNavigator, target -> { });
    }

    public JavaFxLearningWorkspace(DocumentationNavigator documentationNavigator,
                                   SourceNavigator sourceNavigator) {
        this.documentationNavigator = documentationNavigator;
        contentEngine = new LearningContentEngine();
        learningSurface = new JavaFxCeffxLearningSurface();
        renderer = new JavaFxLearningCardRenderer(
                anchor,
                learningSurface,
                contentEngine,
                documentationNavigator,
                sourceNavigator
        );
        learningSurface.setInternalNavigationListener(
                ((JavaFxLearningCardRenderer) this.renderer)::navigateToIdentifier);
    }

    public JavaFxLearningWorkspace(LearningCardRenderer renderer,
                                   DocumentationNavigator documentationNavigator) {
        this.documentationNavigator = documentationNavigator == null ? target -> { } : documentationNavigator;
        contentEngine = new LearningContentEngine();
        learningSurface = null;
        this.renderer = renderer;
    }

    public void openDocumentation(DocumentationTarget target) {
        documentationNavigator.open(target);
    }

    public void setJdkSourceTarget(JdkSourceTarget target) {
        if (renderer instanceof JavaFxLearningCardRenderer legacy) legacy.setJdkSourceTarget(target);
    }

    public void setWorkspaceWindow(javafx.stage.Window window) {
        anchor.setWorkspaceWindow(window);
    }

    public void setMonacoAnchor(int line, int column) {
        if (renderer instanceof MonacoLearningCardRenderer monaco) {
            monaco.setAnchor(line, column);
        }
    }

    public void handleMonacoOverlayEvent(com.eyecode.javafx.monaco.MonacoOverlayEvent event) {
        if (renderer instanceof MonacoLearningCardRenderer monaco) {
            monaco.onOverlayEvent(event);
        }
    }

    public MonacoLearningHoverPipeline createMonacoHoverPipeline() {
        if (!(renderer instanceof MonacoLearningCardRenderer monaco)) {
            throw new IllegalStateException("Monaco learning pipeline requires a Monaco renderer");
        }
        return new MonacoLearningHoverPipeline(
                new JavaFxMonacoLearningIntentTimer(),
                target -> CompletableFuture.supplyAsync(() -> resolveMonacoTarget(target).map(monaco::prepare),
                        monacoLearningExecutor),
                monaco,
                action -> {
                    if (Platform.isFxApplicationThread()) action.run();
                    else {
                        try { Platform.runLater(action); }
                        catch (IllegalStateException ignored) { }
                    }
                });
    }

    private Optional<com.eyecode.learning.model.LearningConcept> resolveMonacoTarget(MonacoLearningTarget target) {
        if (target.tokenType() != TokenType.IDENTIFIER && target.tokenType() != TokenType.KEYWORD) {
            return Optional.empty();
        }
        if (target.tokenType() == TokenType.IDENTIFIER) {
            Optional<com.eyecode.learning.model.LearningConcept> member = memberTargetResolver
                    .resolve(target.documentText(), target.startOffset()).flatMap(jdkCatalog::find);
            if (member.isPresent()) return member;
            Optional<com.eyecode.learning.model.LearningConcept> type = jdkResolver
                    .resolveType(target.documentText(), target.startOffset())
                    .flatMap(resolved -> jdkCatalog.find(resolved.simpleName()));
            if (type.isPresent()) return type;
        }
        Optional<com.eyecode.learning.model.LearningConcept> contextual = syntaxResolver.resolve(target.documentText(), target.startOffset());
        if (contextual.isPresent()) return contextual;
        if (JavaSyntaxLearningResolver.isContextualToken(target.text())) return Optional.empty();
        return syntaxCatalog.find(target.text());
    }

    public LearningHoverController createHoverController(
            CodeArea codeArea,
            Supplier<SyntaxSnapshot> syntaxSupplier
    ) {
        if (disposed) {
            throw new IllegalStateException("Learning workspace is disposed");
        }
        JavaFxLearningHoverSurface surface = new JavaFxLearningHoverSurface(codeArea);
        surface.setPointerObserver(() -> anchor.follow(surface));
        return createHoverController(surface, codeArea::getText, syntaxSupplier, codeArea::getScene);
    }

    public LearningHoverController createHoverController(
            LearningHoverSurface surface,
            Supplier<String> textSupplier,
            Supplier<SyntaxSnapshot> syntaxSupplier,
            Supplier<javafx.scene.Scene> sceneSupplier
    ) {
        if (!(renderer instanceof MonacoLearningCardRenderer)) {
            anchor.follow(surface, () -> sceneSupplier.get() == null ? null : sceneSupplier.get().getWindow());
        }
        var catalog = new DefaultLearningCatalog();
        var conceptEngine = new DefaultLearningConceptEngine(
                List.of(new ClassConceptProvider(catalog)));
        return new LearningHoverController(
                surface,
                renderer,
                new JavaFxLearningHoverScheduler(),
                new DefaultHoverEngine(List.of(new ConceptHoverProvider(conceptEngine))),
                syntaxSupplier,
                identifier -> identifier.startsWith("java/")
                        ? contentEngine.loadHtmlByIdentifier(identifier)
                        : contentEngine.loadHtml(identifier),
                false,
                offset -> jdkResolver.resolveType(textSupplier.get(), offset)
                        .flatMap(type -> jdkCatalog.find(type.simpleName())),
                syntaxCatalog::find,
                offset -> memberTargetResolver.resolve(textSupplier.get(), offset)
                        .flatMap(jdkCatalog::find),
                320L
        );
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        monacoLearningExecutor.shutdownNow();
        renderer.dispose();
        if (learningSurface != null) learningSurface.dispose();
    }

    JavaFxLearningCardRenderer rendererForTest() {
        return (JavaFxLearningCardRenderer) renderer;
    }

    JavaFxCeffxLearningSurface surfaceForTest() {
        return learningSurface;
    }
}
