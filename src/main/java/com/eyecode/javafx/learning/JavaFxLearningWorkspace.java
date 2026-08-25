package com.eyecode.javafx.learning;

import com.eyecode.javafx.ui.toolwindow.content.JavaFxCeffxLearningSurface;
import com.eyecode.learning.catalog.DefaultLearningCatalog;
import com.eyecode.learning.catalog.JdkLearningConceptCatalog;
import com.eyecode.learning.catalog.JavaSyntaxLearningCatalog;
import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.language.documentation.DocumentationAtCaretResolver;
import com.eyecode.language.semantic.JavaMemberTargetResolver;
import com.eyecode.learning.hover.ConceptHoverProvider;
import com.eyecode.learning.hover.DefaultHoverEngine;
import com.eyecode.learning.ui.LearningHoverController;
import com.eyecode.learning.ui.LearningHoverSurface;
import com.eyecode.learning.concepts.DefaultLearningConceptEngine;
import com.eyecode.learning.concepts.providers.ClassConceptProvider;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;

import java.util.List;
import java.util.function.Supplier;

import org.fxmisc.richtext.CodeArea;

public final class JavaFxLearningWorkspace {

    private final JavaFxLearningAnchor anchor = new JavaFxLearningAnchor();
    private final JavaFxCeffxLearningSurface learningSurface;
    private final JavaFxLearningCardRenderer renderer;
    private final LearningContentEngine contentEngine;
    private final DocumentationNavigator documentationNavigator;
    private final JavaMemberTargetResolver memberTargetResolver = new JavaMemberTargetResolver();
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
        learningSurface.setInternalNavigationListener(renderer::navigateToIdentifier);
    }

    public void openDocumentation(DocumentationTarget target) {
        documentationNavigator.open(target);
    }

    public void setJdkSourceTarget(JdkSourceTarget target) {
        renderer.setJdkSourceTarget(target);
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
        anchor.follow(surface, () -> sceneSupplier.get() == null ? null : sceneSupplier.get().getWindow());
        var catalog = new DefaultLearningCatalog();
        var jdkCatalog = new JdkLearningConceptCatalog();
        var syntaxCatalog = new JavaSyntaxLearningCatalog();
        var jdkResolver = new DocumentationAtCaretResolver();
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
                        .flatMap(jdkCatalog::find)
        );
    }

    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        renderer.dispose();
        learningSurface.dispose();
    }

    JavaFxLearningCardRenderer rendererForTest() {
        return renderer;
    }

    JavaFxCeffxLearningSurface surfaceForTest() {
        return learningSurface;
    }
}
