package com.eyecode.ui.web;

import com.eyecode.application.WorkspaceApplication;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import java.nio.file.Path;

import java.util.Objects;

public final class WebShellWorkspaceRuntime implements AutoCloseable {
    private final WorkspaceApplication application;
    private final WebShellWorkspaceController workspaceController;
    private final WebShellDocumentController documentController;
    private final WebShellCompletionController completionController;
    private final WebShellLanguageFeatureController languageFeatureController;
    private final JdtLsProjectService jdt;
    private final WebShellLearningController learningController;
    private final WebShellLessonsController lessonsController;
    private final WebShellDiagnosticsController diagnosticsController;
    private final WebShellExecutionController executionController;
    private boolean closed;

    WebShellWorkspaceRuntime(WorkspaceApplication application,
                             WebShellWorkspaceController workspaceController,
                             WebShellDocumentController documentController,
                             WebShellCompletionController completionController,
                             WebShellLanguageFeatureController languageFeatureController,
                             JdtLsProjectService jdt,
                             WebShellLearningController learningController,
                             WebShellLessonsController lessonsController,
                             WebShellDiagnosticsController diagnosticsController,
                             WebShellExecutionController executionController) {
        this.application = Objects.requireNonNull(application, "application");
        this.workspaceController = Objects.requireNonNull(workspaceController, "workspaceController");
        this.documentController = Objects.requireNonNull(documentController, "documentController");
        this.completionController = Objects.requireNonNull(completionController, "completionController");
        this.languageFeatureController = Objects.requireNonNull(languageFeatureController, "languageFeatureController");
        this.jdt = Objects.requireNonNull(jdt, "jdt");
        this.learningController = Objects.requireNonNull(learningController, "learningController");
        this.lessonsController = Objects.requireNonNull(lessonsController, "lessonsController");
        this.diagnosticsController = Objects.requireNonNull(diagnosticsController, "diagnosticsController");
        this.executionController = Objects.requireNonNull(executionController, "executionController");
    }

    public void openProjectAtStartup(Path root) {
        workspaceController.openProjectAtStartup(Objects.requireNonNull(root, "root"));
    }

    @Override
    public void close() {
        if (closed) return;
        closed = true;
        workspaceController.dispose();
        documentController.dispose();
        completionController.dispose();
        languageFeatureController.dispose();
        jdt.close();
        learningController.dispose();
        lessonsController.closeActiveSession();
        diagnosticsController.dispose();
        executionController.close();
        application.close();
    }
}
