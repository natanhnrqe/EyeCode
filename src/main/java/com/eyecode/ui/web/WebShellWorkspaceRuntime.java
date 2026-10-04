package com.eyecode.ui.web;

import com.eyecode.application.WorkspaceApplication;
import com.eyecode.language.java.lsp.JdtLsProjectDiagnostics;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.language.navigation.NavigationService;
import java.nio.file.Path;

import java.util.Objects;
import java.util.Optional;

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
    private final WebShellNavigationController navigationController;
    private final WebShellJdtDiagnosticsController jdtDiagnosticsController;
    private final WebShellChallengesController challengesController;
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
                              WebShellExecutionController executionController,
                              WebShellNavigationController navigationController,
                              WebShellJdtDiagnosticsController jdtDiagnosticsController) {
        this(application, workspaceController, documentController, completionController, languageFeatureController,
                jdt, learningController, lessonsController, diagnosticsController, executionController,
                navigationController, jdtDiagnosticsController, null);
    }

    WebShellWorkspaceRuntime(WorkspaceApplication application,
                             WebShellWorkspaceController workspaceController,
                             WebShellDocumentController documentController,
                             WebShellCompletionController completionController,
                             WebShellLanguageFeatureController languageFeatureController,
                             JdtLsProjectService jdt,
                             WebShellLearningController learningController,
                             WebShellLessonsController lessonsController,
                              WebShellDiagnosticsController diagnosticsController,
                              WebShellExecutionController executionController,
                              WebShellNavigationController navigationController,
                              WebShellJdtDiagnosticsController jdtDiagnosticsController,
                              WebShellChallengesController challengesController) {
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
        this.navigationController = navigationController;
        this.jdtDiagnosticsController = jdtDiagnosticsController;
        this.challengesController = challengesController;
        if (jdtDiagnosticsController != null) {
            jdt.onJdtDiagnostics((uri, diagnostics) -> jdtDiagnosticsController.publishJdt(uri, diagnostics));
            JdtLsProjectDiagnostics currentDiagnostics = jdt.diagnostics();
            if (currentDiagnostics != null) currentDiagnostics.setListener(
                    (uri, diagnostics) -> jdtDiagnosticsController.publishJdt(uri, diagnostics));
        }
    }

    public void openProjectAtStartup(Path root) {
        workspaceController.openProjectAtStartup(Objects.requireNonNull(root, "root"));
    }

    Optional<NavigationService> navigation() {
        return jdt.navigation();
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
        if (navigationController != null) navigationController.dispose();
        if (jdtDiagnosticsController != null) jdtDiagnosticsController.dispose();
        if (challengesController != null) challengesController.dispose();
        application.close();
    }
}
