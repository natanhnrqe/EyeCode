package com.eyecode.editor.v2.integration;

import com.eyecode.editor.EditorTab;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.ui.RichEditorView;
import com.eyecode.filesystem.FileSystemService;

import javax.swing.JTabbedPane;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class EditorHostPanel extends JTabbedPane {

    private final Map<File, EditorSession> sessions = new HashMap<>();
    private final Map<EditorSession, File> sessionKeys = new HashMap<>();
    private final FileSystemService fileSystemService;
    private EditorSession activeSession;

    public EditorHostPanel(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    public EditorSession openFile(File file) {
        if (file == null) return null;

        File canonicalFile = canonicalize(file);
        EditorSession existing = sessions.get(canonicalFile);
        if (existing != null) {
            activateSession(existing);
            return existing;
        }

        String content = "";
        if (canonicalFile.exists()) {
            try {
                content = fileSystemService.readFile(canonicalFile.toPath());
            } catch (Exception e) {
                content = "";
            }
        }

        EditorDocument document = new EditorDocument(canonicalFile.toPath(), content);
        EditorBuffer buffer = new EditorBuffer(document);
        RichEditorView view = new RichEditorView(buffer);
        EditorTab tab = new EditorTab(canonicalFile);

        EditorSession session = new EditorSession(tab, buffer, view, canonicalFile, dirty -> tab.setDirty(dirty));
        registerSession(canonicalFile, session);

        addTab(canonicalFile.getName(), view);
        activateSession(session);

        return session;
    }

    public EditorSession newUntitled() {
        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);
        RichEditorView view = new RichEditorView(buffer);
        EditorTab tab = new EditorTab(null);

        EditorSession session = new EditorSession(tab, buffer, view, null, dirty -> tab.setDirty(dirty));
        sessionKeys.put(session, null);
        addTab("Untitled", view);
        activateSession(session);

        return session;
    }

    public void closeSession(File file) {
        File canonicalFile = canonicalize(file);
        EditorSession session = sessions.remove(canonicalFile);
        if (session != null) {
            removeSession(session);
        }
    }

    public EditorSession getSession(File file) {
        return sessions.get(canonicalize(file));
    }

    public EditorSession getActiveSession() {
        if (activeSession != null && activeSession.getView().getParent() == this) {
            return activeSession;
        }

        Component selected = getSelectedComponent();
        if (selected == null) return null;
        for (EditorSession session : sessionKeys.keySet()) {
            if (session.getView() == selected) {
                activeSession = session;
                return session;
            }
        }
        return null;
    }

    public RichEditorView getActiveView() {
        EditorSession session = getActiveSession();
        return session != null ? session.getView() : null;
    }

    public boolean isDirty() {
        EditorSession session = getActiveSession();
        return session != null && session.getBuffer().getDocument().isDirty();
    }

    public File getActiveFile() {
        EditorSession session = getActiveSession();
        if (session == null) return null;
        Path source = session.getBuffer().getDocument().getSourceFile();
        return source != null ? source.toFile() : null;
    }

    public String getActiveContent() {
        EditorSession session = getActiveSession();
        return session != null ? session.getBuffer().getDocument().getText() : "";
    }

    public void markActiveClean() {
        EditorSession session = getActiveSession();
        if (session != null) {
            session.getBuffer().getDocument().markClean();
        }
    }

    public void setActiveFile(File file) {
        EditorSession session = getActiveSession();
        if (session != null && file != null) {
            File canonicalFile = canonicalize(file);
            session.getBuffer().getDocument().setSourceFile(canonicalFile.toPath());
            EditorSession previous = sessions.put(canonicalFile, session);
            sessionKeys.put(session, canonicalFile);
            if (previous != null && previous != session) {
                removeSession(previous);
            }
        }
    }

    public void saveActive(File file) {
        EditorSession session = getActiveSession();
        if (session == null) return;
        try {
            File canonicalFile = canonicalize(file);
            fileSystemService.writeFile(canonicalFile.toPath(), session.getBuffer().getDocument().getText());
            session.getBuffer().getDocument().setSourceFile(canonicalFile.toPath());
            session.getBuffer().getDocument().markClean();
            registerSession(canonicalFile, session);
        } catch (Exception ignored) {
        }
    }

    public void closeActive() {
        EditorSession session = getActiveSession();
        if (session == null) return;
        removeSession(session);
    }

    @Override
    public void removeTabAt(int index) {
        Component component = getComponentAt(index);
        EditorSession session = findSession(component);
        if (session != null) {
            removeSession(session);
            return;
        }
        super.removeTabAt(index);
    }

    @Override
    public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        updateActiveSessionFromSelection();
    }

    @Override
    public void setSelectedComponent(Component c) {
        super.setSelectedComponent(c);
        updateActiveSessionFromSelection();
    }

    private void registerSession(File file, EditorSession session) {
        File previousKey = sessionKeys.put(session, file);
        if (previousKey != null && !previousKey.equals(file)) {
            sessions.remove(previousKey);
        }
        if (file != null) {
            sessions.put(file, session);
        }
        activeSession = session;
    }

    private void activateSession(EditorSession session) {
        if (session == null) return;
        activeSession = session;
        setSelectedComponent(session.getView());
    }

    private void removeSession(EditorSession session) {
        if (session == null) return;
        File key = sessionKeys.remove(session);
        if (key != null) {
            sessions.remove(key, session);
        } else {
            sessions.values().removeIf(candidate -> candidate == session);
        }
        if (activeSession == session) {
            activeSession = null;
        }
        session.dispose();
        remove(session.getView());
    }

    private EditorSession findSession(Component component) {
        if (component == null) return null;
        for (EditorSession session : sessionKeys.keySet()) {
            if (session.getView() == component) {
                return session;
            }
        }
        return null;
    }

    private void updateActiveSessionFromSelection() {
        Component selected = getSelectedComponent();
        if (selected == null) {
            activeSession = null;
            return;
        }
        activeSession = findSession(selected);
    }

    private File canonicalize(File file) {
        if (file == null) return null;
        try {
            return file.getCanonicalFile();
        } catch (IOException ignored) {
            return file.getAbsoluteFile();
        }
    }
}
