package com.eyecode.javafx.ui.editor;

import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import com.eyecode.workbench.editor.WorkspaceState;
import com.eyecode.autosave.SavedEvent;
import com.eyecode.autosave.ExternalFileEvent;
import com.eyecode.autosave.ExternalFileState;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.javafx.monaco.JavaFxMonacoEditorSurface;
import com.eyecode.javafx.monaco.MonacoEvent;
import com.eyecode.javafx.monaco.MonacoModelId;
import com.eyecode.javafx.monaco.MonacoPositionAdapter;
import com.eyecode.javafx.learning.JavaFxLearningWorkspace;
import com.eyecode.javafx.learning.MonacoLearningHoverSurface;
import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.learning.ui.LearningHoverController;
import com.eyecode.language.documentation.DocumentationAtCaretResolver;
import com.eyecode.language.documentation.JdkSourceResolver;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.language.symbol.DocumentSemanticModelBuilder;
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
import java.util.function.BiConsumer;
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
    private final JavaFxMonacoEditorSurface monacoSurface;
    private final MonacoLearningHoverSurface monacoHoverSurface;
    private final LearningHoverController learningHoverController;
    private final DocumentationAtCaretResolver documentationAtCaretResolver = new DocumentationAtCaretResolver();
    private final JdkSourceResolver jdkSourceResolver = new JdkSourceResolver();
    private final DocumentSemanticModelBuilder semanticModelBuilder = new DocumentSemanticModelBuilder();
    private final JavaSyntaxAnalyzer syntaxAnalyzer = new JavaSyntaxAnalyzer();
    private BiConsumer<Integer, Integer> caretPositionListener = (line, column) -> { };

    public FxEditorWorkspacePane(EditorManager manager, JavaFxDocumentationWorkspace documentationWorkspace) {
        this(manager, documentationWorkspace, new JavaFxJdkSourceWorkspace(),
                null, null, () -> { }, () -> { }, List::of, project -> { });
    }

    public FxEditorWorkspacePane(EditorManager manager,
                                 JavaFxDocumentationWorkspace documentationWorkspace,
                                 JavaFxJdkSourceWorkspace sourceWorkspace) {
        this(manager, documentationWorkspace, sourceWorkspace,
                null, null, () -> { }, () -> { }, List::of, project -> { });
    }

    public FxEditorWorkspacePane(EditorManager manager,
                                 JavaFxDocumentationWorkspace documentationWorkspace,
                                 JavaFxJdkSourceWorkspace sourceWorkspace,
                                 Runnable newProjectAction,
                                 Runnable openProjectAction,
                                 Supplier<List<ProjectInfo>> recentProjects,
                                 Consumer<ProjectInfo> recentProjectAction) {
        this(manager, documentationWorkspace, sourceWorkspace, null, null,
                newProjectAction, openProjectAction, recentProjects, recentProjectAction);
    }

    public FxEditorWorkspacePane(EditorManager manager,
                                 JavaFxDocumentationWorkspace documentationWorkspace,
                                 JavaFxJdkSourceWorkspace sourceWorkspace,
                                 JavaFxMonacoEditorSurface monacoSurface,
                                 JavaFxLearningWorkspace learningWorkspace,
                                 Runnable newProjectAction,
                                 Runnable openProjectAction,
                                 Supplier<List<ProjectInfo>> recentProjects,
                                 Consumer<ProjectInfo> recentProjectAction) {
        this.manager = manager;
        this.documentationWorkspace = documentationWorkspace;
        this.sourceWorkspace = sourceWorkspace;
        this.monacoSurface = monacoSurface;
        this.monacoHoverSurface = monacoSurface == null || learningWorkspace == null
                ? null : new MonacoLearningHoverSurface(monacoSurface,
                () -> monacoSurface.getScene() == null ? null : monacoSurface.getScene().getWindow(),
                monacoSurface::screenPointForClient);
        this.learningHoverController = monacoHoverSurface == null ? null : learningWorkspace.createHoverController(
                monacoHoverSurface,
                () -> currentMonacoText(),
                () -> currentMonacoSyntax(),
                monacoSurface::getScene);
        if (learningHoverController != null) {
            learningHoverController.setTelemetry(message -> System.out.println(message));
        }
        this.welcomeSurface = new WelcomeProjectSurface(
                newProjectAction == null ? this::showNewProjectSurface : newProjectAction,
                openProjectAction, recentProjects, recentProjectAction);
        this.newProjectSurface = new NewProjectSurface(this::showWelcomeSurface);
        documentationWorkspace.setPresenter(this::openDocumentation);
        sourceWorkspace.setPresenter(this::openSource);
        if (monacoSurface != null) {
            monacoSurface.setEventListener(this::onMonacoEvent);
        }
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
        if (monacoSurface != null && !monacoSurface.containsModel(tab.sourceIdentity())) {
            monacoSurface.openModel(tab.sourceIdentity(), "java", tab.source(), true);
        }
        String id = target.tabId();
        tabs.addSourceTab(id, target.displayName(), () -> {
            if (monacoSurface != null) {
                monacoSurface.closeModel(tab.sourceIdentity());
            }
            sourceWorkspace.close(id);
            tabs.removeSource(id);
            refresh();
        });
        tabs.showSource(id);
        showJdkSource(tab);
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
            Set<String> monacoModelIds = new HashSet<>();
            for (EditorSession session : manager.getSessions()) {
                observeDirty(session);
                syncMonacoModel(session);
                monacoModelIds.add(MonacoModelId.forSession(session));
                models.add(toModel(session));
            }
            if (monacoSurface != null) {
                monacoModelIds.addAll(sourceWorkspace.sourceModelIds());
                monacoSurface.retainModels(monacoModelIds);
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
                showJdkSource(sourceTab);
            }
            return;
        }
        Object nativeView = manager.getNativeView(id);
        if (monacoSurface != null && manager.getSession(id).isPresent()) {
            EditorSession session = manager.getSession(id).orElseThrow();
            monacoSurface.activateModel(MonacoModelId.forSession(session));
            notifyCaretPosition(session);
            contentPane.show(monacoSurface);
            return;
        }
        if (nativeView instanceof Node node) {
            contentPane.show(node);
        }
    }

    private void showJdkSource(JavaFxJdkSourceTab sourceTab) {
        if (monacoSurface == null) {
            contentPane.show(new javafx.scene.control.Label(sourceTab.source()));
            return;
        }
        monacoSurface.activateModel(sourceTab.sourceIdentity());
        int offset = sourceTab.revealedOffset();
        int line = 1;
        int column = 1;
        for (int i = 0; i < Math.min(Math.max(0, offset), sourceTab.source().length()); i++) {
            if (sourceTab.source().charAt(i) == '\n') {
                line++;
                column = 1;
            } else {
                column++;
            }
        }
        monacoSurface.revealPosition(sourceTab.sourceIdentity(), line, column);
        contentPane.show(monacoSurface);
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

    public void setCaretPositionListener(BiConsumer<Integer, Integer> listener) {
        caretPositionListener = listener == null ? (line, column) -> { } : listener;
        EditorSession active = manager.getCurrentSession();
        if (active != null) notifyCaretPosition(active);
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
            if (monacoSurface != null) {
                document.addDocumentChangeListener(event -> {
                    Runnable update = () -> monacoSurface.updateModelContent(
                            MonacoModelId.forSession(session),
                            event.getAfter().getText(), event.getAfter().version(), "host");
                    if (Platform.isFxApplicationThread()) update.run();
                    else {
                        try { Platform.runLater(update); } catch (IllegalStateException ignored) { }
                    }
                });
            }
        });
    }

    private void syncMonacoModel(EditorSession session) {
        if (monacoSurface == null) return;
        manager.getBuffer(session.getSessionId()).ifPresent(buffer -> {
            var snapshot = buffer.getDocument().snapshot();
            String id = MonacoModelId.forSession(session);
            if (!monacoSurface.containsModel(id)) {
                monacoSurface.openModel(id, "java", snapshot.getText(), false);
            } else {
                monacoSurface.updateModelContent(id, snapshot.getText(), snapshot.version(), "host");
            }
        });
    }

    private void onMonacoEvent(MonacoEvent event) {
        if (event == null || event.modelId() == null) return;
        EditorSession session = sessionForModel(event.modelId());
        if (session == null) return;
        if (event.type() == MonacoEvent.Type.CARET_CHANGED) {
            manager.getBuffer(session.getSessionId()).ifPresent(buffer -> {
                var snapshot = buffer.getDocument().snapshot();
                int offset = MonacoPositionAdapter.toOffset(snapshot, event.line(), event.column());
                buffer.moveCaret(buffer.getDocument().positionOf(offset));
                session.setCaretState(buffer.getCaret());
                caretPositionListener.accept(event.line(), event.column());
            });
            return;
        }
        if (event.type() == MonacoEvent.Type.HOVER) {
            manager.getBuffer(session.getSessionId()).ifPresent(buffer -> {
                var snapshot = buffer.getDocument().snapshot();
                int offset = MonacoPositionAdapter.toOffset(snapshot, event.line(), event.column());
                System.out.println("LEARNING_SESSION_OK");
                System.out.println("LEARNING_OFFSET=" + offset);
                if (monacoHoverSurface != null) {
                    monacoHoverSurface.move(offset, event.x(), event.y());
                }
            });
            return;
        }
        if (event.type() == MonacoEvent.Type.HOVER_EXIT) {
            if (monacoHoverSurface != null) monacoHoverSurface.leave();
            return;
        }
        if (event.type() == MonacoEvent.Type.COMMAND) {
            if (event.command() == MonacoEvent.Command.GO_TO_DEFINITION) {
                System.out.println("[MONACO CONSUMER] CTRL_B");
                goToDefinition(session, event.line(), event.column());
            } else if (event.command() == MonacoEvent.Command.DOCUMENTATION) {
                System.out.println("[MONACO CONSUMER] CTRL_Q");
                openDocumentationAtCaret(session, event.line(), event.column());
            }
            return;
        }
        if (event.type() != MonacoEvent.Type.CONTENT_CHANGED) return;
        manager.getSessions().stream()
                .filter(candidate -> MonacoModelId.matches(event.modelId(), candidate.getFile()))
                .findFirst()
                .flatMap(candidate -> manager.getBuffer(candidate.getSessionId()))
                .ifPresent(buffer -> {
                    if (!event.content().equals(buffer.getDocument().snapshot().getText())) {
                        buffer.replaceText(event.content());
                    }
                });
    }

    private EditorSession sessionForModel(String modelId) {
        for (EditorSession session : manager.getSessions()) {
            boolean match = MonacoModelId.matches(modelId, session.getFile());
            if (match) return session;
        }
        return null;
    }

    private void goToDefinition(EditorSession session, int line, int column) {
        manager.getBuffer(session.getSessionId()).ifPresent(buffer -> {
            var snapshot = buffer.getDocument().snapshot();
            int offset = MonacoPositionAdapter.toOffset(snapshot, line, column);
            var location = manager.resolveDefinition(session.getSessionId(), snapshot, offset);
            if (location.isPresent()) {
                int declaration = location.get().declarationRange().startOffset();
                monacoSurface.revealPosition(MonacoModelId.forSession(session),
                        snapshot.lineMap().lineOfOffset(declaration) + 1,
                        snapshot.lineMap().columnOfOffset(declaration) + 1);
                return;
            }
            semanticModelBuilder.build(snapshot).flatMap(model -> documentationAtCaretResolver.resolveType(
                    snapshot.getText(), offset, model.symbolTable())).flatMap(jdkSourceResolver::resolve)
                    .ifPresent(sourceWorkspace::open);
        });
    }

    private void openDocumentationAtCaret(EditorSession session, int line, int column) {
        manager.getBuffer(session.getSessionId()).ifPresent(buffer -> {
            var snapshot = buffer.getDocument().snapshot();
            int offset = MonacoPositionAdapter.toOffset(snapshot, line, column);
            semanticModelBuilder.build(snapshot).flatMap(model -> documentationAtCaretResolver.resolve(
                    snapshot.getText(), offset, model.symbolTable())).ifPresent(documentationWorkspace::open);
        });
    }

    private String currentMonacoText() {
        EditorSession session = manager.getCurrentSession();
        return session == null ? "" : manager.getBuffer(session.getSessionId())
                .map(buffer -> buffer.getDocument().snapshot().getText()).orElse("");
    }

    private void notifyCaretPosition(EditorSession session) {
        manager.getBuffer(session.getSessionId()).ifPresent(buffer -> {
            var snapshot = buffer.getDocument().snapshot();
            int offset = buffer.getDocument().offsetOf(buffer.getCaret());
            caretPositionListener.accept(snapshot.lineMap().lineOfOffset(offset) + 1,
                    snapshot.lineMap().columnOfOffset(offset) + 1);
        });
    }

    private com.eyecode.editor.v2.syntax.SyntaxSnapshot currentMonacoSyntax() {
        EditorSession session = manager.getCurrentSession();
        return session == null ? new com.eyecode.editor.v2.syntax.SyntaxSnapshot(List.of())
                : manager.getBuffer(session.getSessionId()).map(buffer -> (com.eyecode.editor.v2.syntax.SyntaxSnapshot)
                        syntaxAnalyzer.analyze(buffer.getDocument()))
                .orElse(new com.eyecode.editor.v2.syntax.SyntaxSnapshot(List.of()));
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

    public void dispose() {
        if (monacoSurface != null) {
            monacoSurface.dispose();
        }
        if (monacoHoverSurface != null) {
            monacoHoverSurface.dispose();
        }
        if (learningHoverController != null) {
            learningHoverController.dispose();
        }
    }
}
