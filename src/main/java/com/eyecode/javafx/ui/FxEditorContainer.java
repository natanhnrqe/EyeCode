package com.eyecode.javafx.ui;

import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.javafx.editor.view.JavaFxEditorViewFactory;
import com.eyecode.javafx.learning.JavaFxLearningWorkspace;
import com.eyecode.javafx.ui.editor.JavaFxDocumentationWorkspace;
import com.eyecode.javafx.ui.editor.JavaFxJdkSourceWorkspace;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorViewFactory;
import javafx.application.Platform;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.project.ProjectInfo;
import com.eyecode.javafx.ui.editor.FxEditorWorkspacePane;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

public final class FxEditorContainer extends com.eyecode.javafx.designsystem.FxCard {

    private final JavaFxLearningWorkspace learningWorkspace;
    private final JavaFxDocumentationWorkspace documentationWorkspace;
    private final JavaFxJdkSourceWorkspace sourceWorkspace;
    private final EditorManager manager;
    private final FxEditorWorkspacePane workspacePane;

    public FxEditorContainer() {
        this(null, () -> { }, List::of, project -> { });
    }

    public FxEditorContainer(Runnable newProjectAction,
                             Runnable openProjectAction,
                             Supplier<List<ProjectInfo>> recentProjects,
                             Consumer<ProjectInfo> recentProjectAction) {
        getStyleClass().add("editor-card");
        getStyleClass().remove("fx-card");

        EventBus eventBus = new EventBus();
        documentationWorkspace = new JavaFxDocumentationWorkspace();
        sourceWorkspace = new JavaFxJdkSourceWorkspace();
        learningWorkspace = new JavaFxLearningWorkspace(
                documentationWorkspace::open, sourceWorkspace::open);
        EditorViewFactory viewFactory = new JavaFxEditorViewFactory(learningWorkspace, sourceWorkspace::open);
        manager = new EditorManager(
                eventBus, new DefaultFileSystemService(), viewFactory,
                action -> {
                    if (Platform.isFxApplicationThread()) {
                        action.run();
                    } else {
                        try {
                            Platform.runLater(action);
                        } catch (IllegalStateException ignored) {
                        }
                    }
                });

        workspacePane = new FxEditorWorkspacePane(
                manager, documentationWorkspace, sourceWorkspace,
                newProjectAction, openProjectAction, recentProjects, recentProjectAction);
        setContent(workspacePane);
    }

    public void dispose() {
        manager.closeAllSessions();
        manager.shutdownAutosave();
        learningWorkspace.dispose();
        documentationWorkspace.dispose();
        sourceWorkspace.dispose();
    }

    public void openProject(ProjectModel project) {
        manager.closeAllSessions();
        workspacePane.showWelcomeSurface();
    }

    public boolean openFile(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            return false;
        }
        manager.openDocument(file.toAbsolutePath().normalize());
        return true;
    }

    public boolean hasDirtySessions() {
        return manager.getSessions().stream()
                .anyMatch(session -> manager.getBuffer(session.getSessionId())
                        .map(buffer -> buffer.getDocument().isDirty())
                .orElse(false));
    }

    public boolean flushAutosave() {
        return manager.flushAutosave();
    }

    public EditorManager editorManager() {
        return manager;
    }

    public void showWelcomeSurface() {
        workspacePane.showWelcomeSurface();
    }

    public void showNewProjectSurface() {
        workspacePane.showNewProjectSurface();
    }

    public void refreshWelcomeProjects() {
        workspacePane.refreshWelcomeProjects();
    }

    public void setProjectName(String projectName) {
        workspacePane.setProjectName(projectName);
    }

}
