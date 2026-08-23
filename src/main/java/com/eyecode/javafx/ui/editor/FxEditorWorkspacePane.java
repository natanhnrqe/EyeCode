package com.eyecode.javafx.ui.editor;

import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import com.eyecode.workbench.editor.WorkspaceState;
import com.eyecode.autosave.SavedEvent;
import com.eyecode.autosave.ExternalFileEvent;
import com.eyecode.autosave.ExternalFileState;
import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.project.ProjectInfo;
import javafx.scene.Node;
import javafx.application.Platform;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.nio.file.Path;

public final class FxEditorWorkspacePane extends VBox {

    private final EditorManager manager;
    private final FxEditorTabs tabs;
    private final FxEditorContentPane contentPane;
    private final Set<String> dirtyObserved = new HashSet<>();
    private boolean syncing;
    private Path saveFailedFile;
    private Path externalProblemFile;

    private final JavaFxDocumentationWorkspace documentationWorkspace;
    private final JavaFxJdkSourceWorkspace sourceWorkspace;
    private final WelcomeProjectSurface welcomeSurface;
    private final NewProjectSurface newProjectSurface;

    public FxEditorWorkspacePane(EditorManager manager, JavaFxDocumentationWorkspace documentationWorkspace) {
        this(manager, documentationWorkspace, new JavaFxJdkSourceWorkspace(),
                null, () -> { }, List::of, project -> { });
    }

    public FxEditorWorkspacePane(EditorManager manager,
                                 JavaFxDocumentationWorkspace documentationWorkspace,
                                 JavaFxJdkSourceWorkspace sourceWorkspace) {
        this(manager, documentationWorkspace, sourceWorkspace,
                null, () -> { }, List::of, project -> { });
    }

    public FxEditorWorkspacePane(EditorManager manager,
                                 JavaFxDocumentationWorkspace documentationWorkspace,
                                 JavaFxJdkSourceWorkspace sourceWorkspace,
                                 Runnable newProjectAction,
                                 Runnable openProjectAction,
                                 Supplier<List<ProjectInfo>> recentProjects,
                                 Consumer<ProjectInfo> recentProjectAction) {
        this.manager = manager;
        this.documentationWorkspace = documentationWorkspace;
        this.sourceWorkspace = sourceWorkspace;
        this.welcomeSurface = new WelcomeProjectSurface(
                newProjectAction == null ? this::showNewProjectSurface : newProjectAction,
                openProjectAction, recentProjects, recentProjectAction);
        this.newProjectSurface = new NewProjectSurface(this::showWelcomeSurface);
        documentationWorkspace.setPresenter(this::openDocumentation);
        sourceWorkspace.setPresenter(this::openSource);
        manager.addSaveListener(this::onSaveAttempt);
        manager.addExternalFileListener(this::onExternalFileChange);
        getStyleClass().add("editor-workspace-pane");

        this.tabs = new FxEditorTabs();
        this.contentPane = new FxEditorContentPane();
        VBox.setVgrow(contentPane, Priority.ALWAYS);

        tabs.setOnTabSelected(this::selectTab);
        tabs.setOnTabCloseRequested(manager::closeSession);

        getChildren().addAll(tabs, contentPane);

        WorkspaceState state = manager.getWorkspaceState();
        state.addChangeListener(this::refresh);
        state.addActiveSessionListener(session -> refresh());
        refresh();
    }

    private void selectTab(String id) {
        if (id == null) {
            return;
        }
        if (JavaFxDocumentationWorkspace.TAB_ID.equals(id)) {
            mountSelectedContent(id);
        } else if (sourceWorkspace.contains(id)) {
            mountSelectedContent(id);
        } else {
            manager.activateSession(id);
            mountSelectedContent(id);
        }
    }

    private void openSource(JdkSourceTarget target) {
        JavaFxJdkSourceTab tab = sourceWorkspace.ensureTab(target);
        if (tab == null) {
            return;
        }
        tab.reveal(target);
        String id = target.tabId();
        tabs.addSourceTab(id, target.displayName(), () -> {
            sourceWorkspace.close(id);
            tabs.removeSource(id);
            refresh();
        });
        tabs.showSource(id);
        contentPane.show(tab);
    }

    private void openDocumentation(DocumentationTarget target) {
        JavaFxDocumentationTab tab = documentationWorkspace.ensureTab();
        addDocumentationTab();
        tab.open(target);
        tabs.showDocumentation();
        contentPane.show(tab);
    }

    private void addDocumentationTab() {
        tabs.addDocumentationTab(documentationWorkspace.tab(), () -> {
            documentationWorkspace.closeTab();
            tabs.removeDocumentation();
            refresh();
        });
    }

    private void refresh() {
        if (syncing) {
            return;
        }
        syncing = true;
        try {
            List<TabModel> models = new ArrayList<>();
            for (EditorSession session : manager.getSessions()) {
                observeDirty(session);
                models.add(toModel(session));
            }
            EditorSession active = manager.getCurrentSession();
            String activeId = active != null ? active.getSessionId() : null;
            tabs.update(models, activeId);
            String selectedId = tabs.selectedTabId();
            mountSelectedContent(selectedId != null ? selectedId : activeId);
        } finally {
            syncing = false;
        }
    }

    private void mountSelectedContent(String id) {
        if (id == null) {
            showWelcomeSurface();
            return;
        }
        if (JavaFxDocumentationWorkspace.TAB_ID.equals(id)) {
            if (documentationWorkspace.hasTabForTest()) {
                contentPane.show(documentationWorkspace.tab());
            }
            return;
        }
        if (sourceWorkspace.contains(id)) {
            JavaFxJdkSourceTab sourceTab = sourceWorkspace.tab(id);
            if (sourceTab != null) {
                contentPane.show(sourceTab);
            }
            return;
        }
        Object nativeView = manager.getNativeView(id);
        if (nativeView instanceof Node node) {
            contentPane.show(node);
        }
    }

    public void showWelcomeSurface() {
        contentPane.show(welcomeSurface);
    }

    public void showNewProjectSurface() {
        contentPane.show(newProjectSurface);
    }

    public void refreshWelcomeProjects() {
        welcomeSurface.refreshRecentProjects();
    }

    public void setProjectName(String projectName) {
        welcomeSurface.setProjectName(projectName);
    }

    private void observeDirty(EditorSession session) {
        String sessionId = session.getSessionId();
        if (dirtyObserved.contains(sessionId)) {
            return;
        }
        dirtyObserved.add(sessionId);
        manager.getBuffer(sessionId).ifPresent(buffer -> {
            EditorDocument document = buffer.getDocument();
            document.addDirtyChangeListener(dirty -> refresh());
        });
    }

    private TabModel toModel(EditorSession session) {
        boolean dirty = manager.getBuffer(session.getSessionId())
                .map(EditorBuffer::getDocument)
                .map(EditorDocument::isDirty)
                .orElse(false);
        return new TabModel(
                session.getSessionId(),
                session.getDisplayName(),
                dirty,
                session.isPinned(),
                session.isPreview(),
                saveFailedFile != null && session.getFile() != null
                        && saveFailedFile.equals(session.getFile().toAbsolutePath().normalize())
                        || externalProblemFile != null && session.getFile() != null
                        && externalProblemFile.equals(session.getFile().toAbsolutePath().normalize()));
    }

    private void onSaveAttempt(SavedEvent event) {
        if (event == null || event.path() == null) return;
        Runnable update = () -> {
            saveFailedFile = event.succeeded()
                    ? null
                    : event.path().toAbsolutePath().normalize();
            refresh();
        };
        if (Platform.isFxApplicationThread()) {
            update.run();
        } else {
            try {
                Platform.runLater(update);
            } catch (IllegalStateException ignored) {
            }
        }
    }

    private void onExternalFileChange(ExternalFileEvent event) {
        if (event == null || event.path() == null) return;
        externalProblemFile = event.path().toAbsolutePath().normalize();
        refresh();
        EditorSession session = manager.getWorkspaceState().findSessionByFile(externalProblemFile).orElse(null);
        if (session == null) return;
        if (event.state() == ExternalFileState.CONFLICT) {
            ButtonType reload = new ButtonType("Reload from Disk");
            ButtonType keep = new ButtonType("Keep My Changes");
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "File changed outside EyeCode.", reload, keep);
            alert.setTitle("External File Change");
            alert.showAndWait().ifPresent(choice -> {
                if (choice == reload) {
                    manager.reloadFromDisk(session.getSessionId());
                    externalProblemFile = null;
                    refresh();
                } else if (choice == keep) {
                    if (manager.keepLocalChanges(session.getSessionId())) {
                        externalProblemFile = null;
                        refresh();
                    }
                }
            });
        } else if (event.state() == ExternalFileState.DELETED) {
            Alert alert = new Alert(Alert.AlertType.WARNING,
                    "File deleted outside EyeCode. The editor contents remain available.", ButtonType.OK);
            alert.setTitle("External File Change");
            alert.showAndWait();
        }
    }

    FxEditorTabs tabsForTest() {
        return tabs;
    }

    FxEditorContentPane contentPaneForTest() {
        return contentPane;
    }

    WelcomeProjectSurface welcomeSurfaceForTest() {
        return welcomeSurface;
    }

    NewProjectSurface newProjectSurfaceForTest() {
        return newProjectSurface;
    }
}
