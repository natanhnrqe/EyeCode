package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.EditorActivatedEvent;
import com.eyecode.eventbus.events.FileClosedEvent;
import com.eyecode.eventbus.events.FileOpenedEvent;
import com.eyecode.filesystem.FileSystemService;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.LexerEventBridge;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class EditorManager {

    private final EventBus eventBus;
    private final FileSystemService fileSystemService;
    private final EditorViewFactory viewFactory;
    private final LexerEventBridge lexerEventBridge;

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
        this.eventBus = eventBus;
        this.fileSystemService = fileSystemService;
        this.viewFactory = viewFactory;
        this.lexerEventBridge = eventBus != null
                ? new LexerEventBridge(new JavaLexerService(), eventBus)
                : null;
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

    public void closeSession(String sessionId) {
        EditorSession session = sessionsById.get(sessionId);
        if (session == null || session.getState() == SessionState.DISPOSED) {
            return;
        }

        EditorView view = viewsBySession.get(sessionId);
        EditorBuffer buffer = buffersBySession.get(sessionId);

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
    }

    public void activateSession(String sessionId) {
        EditorSession session = sessionsById.get(sessionId);
        if (session == null) {
            return;
        }

        EditorSession previous = workspaceState.getActiveSession();
        if (previous != null && previous != session && previous.getState() == SessionState.ACTIVE) {
            previous.setState(SessionState.INACTIVE);
        }
        session.setState(SessionState.ACTIVE);
        workspaceState.setActiveSession(session);
        history.recordActivation(EditorViewport.initial(session.getFile()));

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

        selectionService.bind(session, buffer);
        workspaceState.addSession(session);
        session.setState(SessionState.VISIBLE);

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
}
