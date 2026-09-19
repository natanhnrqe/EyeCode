package com.eyecode.ui.web;

import com.eyecode.application.WorkspaceApplication;
import com.eyecode.application.WorkspaceProjects;
import com.eyecode.application.ProjectExplorerQuery;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.ExtensionDocumentLanguageResolver;
import com.eyecode.language.LanguageId;
import com.eyecode.language.completion.CompletionService;
import com.eyecode.language.diagnostics.DiagnosticsService;
import com.eyecode.diagnostics.JavaDiagnosticsProvider;
import com.eyecode.language.java.completion.JavaCompletionProvider;
import com.eyecode.language.java.JavaEditorIntelligence;
import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.project.MavenProjectCreationService;
import com.eyecode.project.ProjectFileOperationService;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.runtime.RunService;
import com.eyecode.terminal.TerminalService;
import com.eyecode.workbench.editor.EditorManager;

import java.util.function.Consumer;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class WebShellWorkspaceComposition {
    private WebShellWorkspaceComposition() {
    }

    public static WebShellWorkspaceRuntime create(WebShellSurface surface) {
        return create(surface, target -> { }, null, WebShellNativeUi.unavailable());
    }

    public static WebShellWorkspaceRuntime create(WebShellSurface surface,
                                                  Consumer<DocumentationTarget> documentationOpener,
                                                  WebShellNativeUi nativeUi) {
        return create(surface, documentationOpener, null, nativeUi);
    }

    public static WebShellWorkspaceRuntime create(WebShellSurface surface,
                                                  WebShellDocumentationHost documentationHost,
                                                  WebShellNativeUi nativeUi) {
        Consumer<DocumentationTarget> opener = documentationHost == null
                ? target -> { } : documentationHost::open;
        return create(surface, opener, documentationHost, nativeUi);
    }

    private static WebShellWorkspaceRuntime create(WebShellSurface surface,
                                                   Consumer<DocumentationTarget> documentationOpener,
                                                   WebShellDocumentationHost documentationHost,
                                                   WebShellNativeUi nativeUi) {
        EventBus eventBus = new EventBus();
        ProjectFileOperationService fileOperations = new ProjectFileOperationService();
        EditorManager editorManager = new EditorManager(eventBus, new DefaultFileSystemService(),
                new WebShellEditorViewFactory(), Runnable::run, fileOperations, new JavaEditorIntelligence(eventBus));
        ProjectLifecycleService projectLifecycleService = new ProjectLifecycleService();
        RunService runService = new RunService(projectLifecycleService);
        runService.setBeforeRunFlush(editorManager::flushAutosave);
        TerminalService terminalService = new TerminalService();
        MavenProjectCreationService projectCreationService = new MavenProjectCreationService();
        WorkspaceApplication application = new WorkspaceApplication(editorManager, projectLifecycleService,
                runService, terminalService);

        DocumentLanguageResolver languageResolver = new ExtensionDocumentLanguageResolver(
                Map.of(LanguageId.JAVA, Set.of("java")));
        DiagnosticsService diagnostics = new DiagnosticsService(languageResolver, List.of(new JavaDiagnosticsProvider()));
        CompletionService completion = new CompletionService(languageResolver, List.of(new JavaCompletionProvider()));
        WebShellDiagnosticsController diagnosticsController = new WebShellDiagnosticsController(surface, diagnostics);
        WebShellExecutionController executionController = new WebShellExecutionController(surface,
                projectLifecycleService, runService, terminalService);
        WorkspaceProjects projects = new WorkspaceProjects(projectLifecycleService, editorManager,
                runService, projectCreationService);
        ProjectExplorerQuery explorer = new ProjectExplorerQuery();
        WebShellNativeFileSelection selection = nativeUi == null ? WebShellNativeUi.unavailable() : nativeUi;
        WebShellDocumentController documentController = new WebShellDocumentController(surface,
                documentationOpener, documentationHost, selection, editorManager, diagnosticsController, languageResolver);
        WebShellWorkspaceController workspaceController = new WebShellWorkspaceController(surface,
                selection, editorManager, projects, explorer, fileOperations, documentController, executionController);
        WebShellCompletionController completionController = new WebShellCompletionController(surface, editorManager,
                completion);
        WebShellLearningController learningController = new WebShellLearningController(surface, editorManager,
                documentController::openDocumentationTarget, documentController::openJdkSource);
        WebShellLessonsController lessonsController = new WebShellLessonsController(surface);
        return new WebShellWorkspaceRuntime(application, workspaceController, documentController, completionController,
                learningController, lessonsController, diagnosticsController, executionController);
    }
}
