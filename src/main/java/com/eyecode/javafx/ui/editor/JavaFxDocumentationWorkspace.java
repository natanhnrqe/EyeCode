package com.eyecode.javafx.ui.editor;

import com.eyecode.learning.content.DocumentationTarget;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class JavaFxDocumentationWorkspace {

    public static final String TAB_ID = "__eyecode_documentation__";

    private JavaFxDocumentationTab tab;
    private JavaFxDocumentationSurface surface;
    private final Supplier<JavaFxDocumentationSurface> surfaceFactory;
    private Consumer<DocumentationTarget> presenter;
    private boolean disposed;

    public JavaFxDocumentationWorkspace() {
        this(JavaFxDocumentationSurface::new);
    }

    JavaFxDocumentationWorkspace(JavaFxDocumentationSurface surface) {
        this(() -> surface);
    }

    JavaFxDocumentationWorkspace(Supplier<JavaFxDocumentationSurface> surfaceFactory) {
        this.surfaceFactory = surfaceFactory;
        this.surface = surfaceFactory.get();
        tab = new JavaFxDocumentationTab(surface);
    }

    public void open(DocumentationTarget target) {
        if (!disposed && target != null) {
            if (presenter != null) {
                presenter.accept(target);
            } else {
                ensureTab().open(target);
            }
        }
    }

    public void setPresenter(Consumer<DocumentationTarget> presenter) {
        this.presenter = presenter;
    }

    JavaFxDocumentationTab ensureTab() {
        if (tab == null) {
            surface = surfaceFactory.get();
            tab = new JavaFxDocumentationTab(surface);
        }
        return tab;
    }

    boolean hasTabForTest() {
        return tab != null;
    }

    public JavaFxDocumentationTab tab() {
        return ensureTab();
    }

    void closeTab() {
        if (tab != null) {
            tab.dispose();
            tab = null;
            surface = null;
        }
    }

    public void dispose() {
        if (!disposed) {
            disposed = true;
            closeTab();
        }
    }

    JavaFxDocumentationTab tabForTest() {
        return tab;
    }
}
