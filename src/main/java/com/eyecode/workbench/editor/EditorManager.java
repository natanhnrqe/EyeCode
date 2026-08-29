package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.autosave.AutoSaveManager;
import com.eyecode.autosave.SavedEvent;
import com.eyecode.autosave.ExternalFileState;
import com.eyecode.autosave.ExternalFileEvent;
import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.EditorActivatedEvent;
import com.eyecode.eventbus.events.FileClosedEvent;
import com.eyecode.eventbus.events.FileOpenedEvent;
import com.eyecode.filesystem.FileSystemService;
import com.eyecode.filesystem.ExternalFileWatcher;
import com.eyecode.project.ProjectFileOperationService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.eventbus.events.ProjectRefreshEvent;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.LexerEventBridge;
import com.eyecode.language.semantic.DefinitionAtCaretResolver;
import com.eyecode.language.semantic.DefinitionLocation;
import com.eyecode.language.symbol.DocumentSemanticModelBuilder;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.SymbolTable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

public final class EditorManager {

    private final EventBus eventBus;
    private final FileSystemService fileSystemService;
    private final EditorViewFactory viewFactory;
    private final JavaLexerService lexerService = new JavaLexerService();
    private final DocumentSemanticModelBuilder semanticModelBuilder = new DocumentSemanticModelBuilder(lexerService);
    private final DefinitionAtCaretResolver definitionAtCaretResolver = new DefinitionAtCaretResolver();
    private final LexerEventBridge lexerEventBridge;
    private final AutoSaveManager autoSaveManager;
    private final Consumer<Runnable> stateDispatcher;
    private final ExternalFileWatcher externalFileWatcher;
    private final ProjectFileOperationService fileOperationService = new ProjectFileOperationService();
    private final List<Consumer<ExternalFileEvent>> externalFileListeners = new java.util.concurrent.CopyOnWriteArrayList<>();

    private final WorkspaceState workspaceState = new WorkspaceState();
    private final EditorHistory history = new EditorHistory();
    private final EditorSelectionService selectionService = new EditorSelectionService();

    private final Map<String, EditorSession> sessionsById = new LinkedHashMap<>();
    private final Map<String, EditorBuffer> buffersBySession = new HashMap<>();
    private final Map<String, EditorView> viewsBySession = new HashMap<>();
    private final Map<String, EditorDocument> documentsBySession = new HashMap<>();

    public EditorManager(EventBus eventBus,
                         FileSystemService fileSystemService,
                         EditorViewFactory viewFactory) {
        this(eventBus, fileSystemService, viewFactory, Runnable::run);
    }

    public EditorManager(EventBus eventBus,
                         FileSystemService fileSystemService,
                         EditorViewFactory viewFactory,
                         Consumer<Runnable> stateDispatcher) {
        this.eventBus = eventBus;
        this.fileSystemService = fileSystemService;
        this.viewFactory = viewFactory;
        this.stateDispatcher = stateDispatcher == null ? Runnable::run : stateDispatcher;
        this.lexerEventBridge = eventBus != null
                ? new LexerEventBridge(lexerService, eventBus)
                : null;
        this.autoSaveManager = new AutoSaveManager(fileSystemService, stateDispatcher);
        ExternalFileWatcher watcher = new ExternalFileWatcher();
        watcher.addListener(this::onExternalPathChanged);
        this.externalFileWatcher = watcher;
    }

    public EditorSession openDocument(Path file) {
        if (file == null) {
            throw new IllegalArgumentException("openDocument exige um arquivo.");
        }
        Optional<EditorSession> existing = workspaceState.findSessionByFile(file);
        if (existing.isPresent()) {
            activateSession(existing.get().getSessionId());
            return existing.get();
        }
        try {
            String content = fileSystemService.readFile(file);
            return openDocument(file, content);
        } catch (IOException e) {
            throw new IllegalStateException("Falha ao ler arquivo: " + file, e);
        }
    }

    public EditorSession openDocument(Path file, String content) {
        if (file != null) {
            Optional<EditorSession> existing = workspaceState.findSessionByFile(file);
            if (existing.isPresent()) {
                activateSession(existing.get().getSessionId());
                return existing.get();
            }
        }
        EditorDocument document = new EditorDocument(file, content == null ? "" : content);
        EditorBuffer buffer = new EditorBuffer(document, eventBus);
        return createSession(file, document, buffer);
    }

    public boolean closeSession(String sessionId) {
        return closeSession(sessionId, true);
    }

    private boolean closeSession(String sessionId, boolean persist) {
        EditorSession session = sessionsById.get(sessionId);
        if (session == null || session.getState() == SessionState.DISPOSED) {
            return false;
        }

        EditorView view = viewsBySession.get(sessionId);
        EditorBuffer buffer = buffersBySession.get(sessionId);
        EditorDocument document = documentsBySession.get(sessionId);

        if (persist && document != null && !autoSaveManager.saveNow(document)) {
            return false;
        }

        EditorViewport snapshot = selectionService.captureViewport(session);
        selectionService.unbind(session, buffer);
        if (view != null) {
            view.dispose();
        }

        int closedIndex = workspaceState.indexOf(session);
        workspaceState.removeSession(session);
        sessionsById.remove(sessionId);
        viewsBySession.remove(sessionId);
        buffersBySession.remove(sessionId);
        documentsBySession.remove(sessionId);

        if (document != null) {
            lexerService.invalidateSession(document.sessionId());
        }

        session.setState(SessionState.DISPOSED);
        history.recordClose(snapshot);

        if (eventBus != null) {
            eventBus.publish(new FileClosedEvent(fileOf(session)));
        }

        if (workspaceState.getActiveSession() == null) {
            List<EditorSession> remaining = workspaceState.getOpenSessions();
            if (!remaining.isEmpty()) {
                int next = Math.min(closedIndex, remaining.size() - 1);
                activateSession(remaining.get(next).getSessionId());
            }
        }
        return true;
    }

    public boolean deletePath(ProjectModel project, Path target) {
        Path safe;
        try {
            safe = fileOperationService.requireTarget(project, target);
            for (EditorSession session : List.copyOf(sessionsById.values())) {
                if (session.getFile() != null && session.getFile().toAbsolutePath().normalize().startsWith(safe)) {
                    autoSaveManager.unregister(documentsBySession.get(session.getSessionId()));
                }
            }
            fileOperationService.delete(project, safe);
            for (EditorSession session : List.copyOf(sessionsById.values())) {
                if (session.getFile() != null && session.getFile().toAbsolutePath().normalize().startsWith(safe)) {
                    closeSession(session.getSessionId(), false);
                }
            }
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            return false;
        }
    }

    public boolean renamePath(ProjectModel project, Path target, String newName) {
        try {
            Path oldPath = fileOperationService.requireTarget(project, target);
            ProjectFileOperationService.RenameResult result = fileOperationService.rename(project, oldPath, newName);
            for (EditorSession session : List.copyOf(sessionsById.values())) {
                Path file = session.getFile();
                if (file == null) continue;
                Path normalized = file.toAbsolutePath().normalize();
                if (normalized.equals(result.oldPath()) || normalized.startsWith(result.oldPath())) {
                    Path suffix = result.oldPath().relativize(normalized);
                    Path next = result.newPath().resolve(suffix).normalize();
                    EditorDocument document = documentsBySession.get(session.getSessionId());
                    autoSaveManager.rebind(document, next);
                    session.setFile(next);
                    if (result.source() != null && normalized.equals(result.oldPath())) {
                        document.setText(result.source());
                        document.markClean();
                    }
                }
            }
            return true;
        } catch (IOException | IllegalArgumentException exception) {
            return false;
        }
    }

    public void closeAllSessions() {
        for (String sessionId : List.copyOf(sessionsById.keySet())) {
            closeSession(sessionId);
        }
    }

    public boolean flushAutosave() {
        if (documentsBySession.values().stream().anyMatch(autoSaveManager::hasExternalConflict)) {
            return false;
        }
        return autoSaveManager.saveAll();
    }

    public boolean flushSession(String sessionId) {
        EditorDocument document = documentsBySession.get(sessionId);
        return document == null || autoSaveManager.saveNow(document);
    }

    public boolean saveAs(String sessionId, Path target) {
        EditorDocument document = documentsBySession.get(sessionId);
        return document != null && target != null && autoSaveManager.saveAs(document, target);
    }

    public boolean hasSaveFailure(String sessionId) {
        EditorDocument document = documentsBySession.get(sessionId);
        return document != null && autoSaveManager.hasSaveFailure(document);
    }

    public boolean hasExternalConflict(String sessionId) {
        EditorDocument document = documentsBySession.get(sessionId);
        return document != null && autoSaveManager.hasExternalConflict(document);
    }

    public ExternalFileState externalState(String sessionId) {
        EditorDocument document = documentsBySession.get(sessionId);
        return autoSaveManager.externalState(document);
    }

    public boolean keepLocalChanges(String sessionId) {
        EditorDocument document = documentsBySession.get(sessionId);
        return document != null && autoSaveManager.keepLocalChanges(document);
    }

    public boolean reloadFromDisk(String sessionId) {
        EditorDocument document = documentsBySession.get(sessionId);
        return document != null && autoSaveManager.reloadFromDisk(document);
    }

    public void watchProject(Path root) {
        if (externalFileWatcher == null || root == null) return;
        externalFileWatcher.clearRoots();
        try {
            externalFileWatcher.watchRoot(root);
        } catch (IOException ignored) {
        }
    }

    public void addSaveListener(Consumer<SavedEvent> listener) {
        autoSaveManager.addSaveListener(listener);
    }

    public void removeSaveListener(Consumer<SavedEvent> listener) {
        autoSaveManager.removeSaveListener(listener);
    }

    public void shutdownAutosave() {
        autoSaveManager.shutdown();
        if (externalFileWatcher != null) {
            externalFileWatcher.close();
        }
    }

    public void addExternalFileListener(Consumer<ExternalFileEvent> listener) {
        if (listener != null) externalFileListeners.add(listener);
    }

    public void removeExternalFileListener(Consumer<ExternalFileEvent> listener) {
        externalFileListeners.remove(listener);
    }

    public void activateSession(String sessionId) {
        EditorSession session = sessionsById.get(sessionId);
        if (session == null) {
            return;
        }

        EditorSession previous = workspaceState.getActiveSession();
        if (previous != null && previous != session && previous.getState() == SessionState.ACTIVE) {
            previous.setState(SessionState.INACTIVE);
            EditorDocument previousDocument = documentsBySession.get(previous.getSessionId());
            if (previousDocument != null) {
                lexerService.deactivateSession(previousDocument.sessionId());
            }
        }
        session.setState(SessionState.ACTIVE);
        workspaceState.setActiveSession(session);
        history.recordActivation(EditorViewport.initial(session.getFile()));
        EditorDocument document = documentsBySession.get(sessionId);
        if (document != null) {
            lexerService.activateSession(document.sessionId());
        }

        if (eventBus != null) {
            eventBus.publish(new EditorActivatedEvent(session));
        }
    }

    public EditorSession getCurrentSession() {
        return workspaceState.getActiveSession();
    }

    public List<EditorSession> getSessions() {
        return workspaceState.getOpenSessions();
    }

    public Optional<EditorSession> getSession(String sessionId) {
        return Optional.ofNullable(sessionsById.get(sessionId));
    }

    public Optional<EditorBuffer> getBuffer(String sessionId) {
        return Optional.ofNullable(buffersBySession.get(sessionId));
    }

    public Optional<EditorView> getView(String sessionId) {
        return Optional.ofNullable(viewsBySession.get(sessionId));
    }

    public Object getNativeView(String sessionId) {
        EditorView view = viewsBySession.get(sessionId);
        return view != null ? view.getNativeView() : null;
    }

    public WorkspaceState getWorkspaceState() {
        return workspaceState;
    }

    public EditorHistory getHistory() {
        return history;
    }

    public EditorSelectionService getSelectionService() {
        return selectionService;
    }

    public Optional<DefinitionLocation> resolveDefinition(String sessionId, int caretOffset) {
        if (sessionId == null) {
            return Optional.empty();
        }
        EditorDocument document = documentsBySession.get(sessionId);
        if (document == null) {
            return Optional.empty();
        }
        return resolveDefinition(sessionId, document.snapshot(), caretOffset);
    }

    public Optional<DefinitionLocation> resolveDefinition(String sessionId,
                                                          DocumentSnapshot snapshot,
                                                          int caretOffset) {
        if (sessionId == null || snapshot == null) {
            return Optional.empty();
        }
        EditorDocument document = documentsBySession.get(sessionId);
        if (document == null || !document.sessionId().equals(snapshot.sessionId())
                || document.currentVersion() != snapshot.version()) {
            return Optional.empty();
        }
        String source = snapshot.getText();
        if (caretOffset < 0 || caretOffset > source.length()) {
            return Optional.empty();
        }
        Optional<SemanticModelSnapshot> model = semanticModelBuilder.build(snapshot);
        if (model.isEmpty()) {
            return Optional.empty();
        }
        return definitionAtCaretResolver.resolve(source, caretOffset, model.get().symbolTable());
    }

    private EditorSession createSession(Path file, EditorDocument document, EditorBuffer buffer) {
        String sessionId = UUID.randomUUID().toString();
        String documentId = UUID.randomUUID().toString();
        EditorSession session = new EditorSession(sessionId, documentId, file, OpenOptions.standard());

        session.setState(SessionState.LOADED);
        EditorView view = viewFactory.create(buffer);

        documentsBySession.put(sessionId, document);
        buffersBySession.put(sessionId, buffer);
        viewsBySession.put(sessionId, view);
        sessionsById.put(sessionId, session);

        view.bindNavigation(this, sessionId);
        selectionService.bind(session, buffer);
        workspaceState.addSession(session);
        session.setState(SessionState.VISIBLE);
        autoSaveManager.register(document);
        if (externalFileWatcher != null && file != null) {
            try {
                externalFileWatcher.watchFile(file);
            } catch (IOException ignored) {
            }
        }

        if (eventBus != null) {
            eventBus.publish(new FileOpenedEvent(fileOf(session)));
        }
        activateSession(sessionId);
        return session;
    }

    private java.io.File fileOf(EditorSession session) {
        Path file = session.getFile();
        return file != null ? file.toFile() : null;
    }

    private Optional<SymbolTable> buildSymbolTable(EditorDocument document) {
        Optional<SemanticModelSnapshot> semantic = semanticModelBuilder.build(document);
        return semantic.map(SemanticModelSnapshot::symbolTable);
    }

    private void onExternalPathChanged(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        ExternalFileState affectedState = ExternalFileState.SYNCED;
        for (EditorDocument document : List.copyOf(documentsBySession.values())) {
            Path source = document.getSourceFile();
            if (source != null && source.toAbsolutePath().normalize().equals(normalized)) {
                affectedState = autoSaveManager.synchronizeExternal(document);
            }
        }
        ProjectRefreshEvent.Kind kind = Files.isDirectory(normalized)
                ? ProjectRefreshEvent.Kind.DIRECTORY_CREATED
                : Files.exists(normalized)
                        ? ProjectRefreshEvent.Kind.FILE_MODIFIED
                        : ProjectRefreshEvent.Kind.FILE_DELETED;
        if (eventBus != null) {
            stateDispatcher.accept(() -> eventBus.publish(new ProjectRefreshEvent(kind, normalized)));
        }
        ExternalFileEvent event = new ExternalFileEvent(normalized, affectedState);
        stateDispatcher.accept(() -> {
            for (Consumer<ExternalFileEvent> listener : externalFileListeners) {
                listener.accept(event);
            }
        });
    }
}
