package com.eyecode.workbench.editor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public final class WorkspaceState {

    private final List<EditorSession> openSessions = new ArrayList<>();
    private EditorSession activeSession;
    private final List<Runnable> changeListeners = new ArrayList<>();
    private final List<Consumer<EditorSession>> activeListeners = new ArrayList<>();

    void addSession(EditorSession session) {
        if (session != null && !openSessions.contains(session)) {
            openSessions.add(session);
            notifyChanged();
        }
    }

    void removeSession(EditorSession session) {
        boolean removed = openSessions.remove(session);
        if (removed && activeSession == session) {
            activeSession = null;
        }
        if (removed) {
            notifyChanged();
        }
    }

    void setActiveSession(EditorSession session) {
        if (activeSession == session) {
            return;
        }
        activeSession = session;
        notifyChanged();
    }

    int indexOf(EditorSession session) {
        return openSessions.indexOf(session);
    }

    public List<EditorSession> getOpenSessions() {
        return List.copyOf(openSessions);
    }

    public EditorSession getActiveSession() {
        return activeSession;
    }

    public Optional<EditorSession> findSessionByFile(Path file) {
        if (file == null) {
            return Optional.empty();
        }
        return openSessions.stream()
                .filter(session -> file.equals(session.getFile()))
                .findFirst();
    }

    public void addChangeListener(Runnable listener) {
        if (listener != null && !changeListeners.contains(listener)) {
            changeListeners.add(listener);
        }
    }

    public void removeChangeListener(Runnable listener) {
        changeListeners.remove(listener);
    }

    public void addActiveSessionListener(Consumer<EditorSession> listener) {
        if (listener != null && !activeListeners.contains(listener)) {
            activeListeners.add(listener);
        }
    }

    public void removeActiveSessionListener(Consumer<EditorSession> listener) {
        activeListeners.remove(listener);
    }

    private void notifyChanged() {
        for (Runnable listener : List.copyOf(changeListeners)) {
            listener.run();
        }
        for (Consumer<EditorSession> listener : List.copyOf(activeListeners)) {
            listener.accept(activeSession);
        }
    }
}
