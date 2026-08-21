package com.eyecode.javafx.learning;

import com.eyecode.javafx.ui.toolwindow.content.JavaFxCeffxLearningSurface;
import com.eyecode.learning.catalog.DefaultLearningCatalog;
import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.hover.ConceptHoverProvider;
import com.eyecode.learning.hover.DefaultHoverEngine;
import com.eyecode.learning.ui.LearningHoverController;
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
    private boolean disposed;

    public JavaFxLearningWorkspace() {
        this(target -> { });
    }

    public JavaFxLearningWorkspace(DocumentationNavigator documentationNavigator) {
        contentEngine = new LearningContentEngine();
        learningSurface = new JavaFxCeffxLearningSurface();
        renderer = new JavaFxLearningCardRenderer(
                anchor,
                learningSurface,
                contentEngine,
                documentationNavigator
        );
        learningSurface.setInternalNavigationListener(renderer::navigateToIdentifier);
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
                false
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
