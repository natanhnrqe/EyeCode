package com.eyecode.editor.v2.integration;

import com.eyecode.autosave.AutoSaveManager;
import com.eyecode.editor.EditorTab;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.ui.RichEditorView;
import com.eyecode.filesystem.FileSystemService;

import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class EditorHostPanel extends JTabbedPane {

    private final Map<File, EditorSession> sessions = new HashMap<>();
    private final Map<EditorSession, File> sessionKeys = new HashMap<>();
    private final Map<Component, EditorSession> componentSessions = new HashMap<>();
    private final FileSystemService fileSystemService;
    private AutoSaveManager autoSaveManager;
    private EditorSession activeSession;

    public EditorHostPanel(FileSystemService fileSystemService) {
        this(fileSystemService, null);
    }

    public EditorHostPanel(FileSystemService fileSystemService, AutoSaveManager autoSaveManager) {
        this.fileSystemService = fileSystemService;
        this.autoSaveManager = autoSaveManager;
    }

    public void setAutoSaveManager(AutoSaveManager autoSaveManager) {
        this.autoSaveManager = autoSaveManager;
    }

    public EditorSession openFile(File file) {
        if (file == null) return null;

        File canonicalFile = canonicalize(file);
        EditorSession existing = sessions.get(canonicalFile);
        if (existing != null) {
            existing.restoreViewState();
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

        EditorSession[] box = new EditorSession[1];
        box[0] = new EditorSession(tab, buffer, view, canonicalFile, dirty -> {
            tab.setDirty(dirty);
            EditorSession s = box[0];
            if (s != null) refreshTitle(s);
        });
        EditorSession session = box[0];
        registerSession(canonicalFile, session);

        addTab(canonicalFile.getName(), view);
        activateSession(session);
        wireAutoSave(session);

        return session;
    }

    public EditorSession newUntitled() {
        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);
        RichEditorView view = new RichEditorView(buffer);
        EditorTab tab = new EditorTab(null);

        EditorSession[] box = new EditorSession[1];
        box[0] = new EditorSession(tab, buffer, view, null, dirty -> {
            tab.setDirty(dirty);
            EditorSession s = box[0];
            if (s != null) refreshTitle(s);
        });
        EditorSession session = box[0];
        sessionKeys.put(session, null);
        addTab("Untitled", view);
        activateSession(session);
        wireAutoSave(session);

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
        Component selected = getSelectedComponent();
        if (selected == null) {
            activeSession = null;
            return null;
        }

        if (activeSession != null && activeSession.getView() == selected) {
            return activeSession;
        }

        activeSession = componentSessions.get(selected);
        return activeSession;
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
            File previousKey = sessionKeys.put(session, canonicalFile);
            if (previousKey != null && !previousKey.equals(canonicalFile)) {
                sessions.remove(previousKey, session);
            }
            EditorSession previous = sessions.put(canonicalFile, session);
            if (previous != null && previous != session) {
                removeSession(previous);
            }
            activeSession = session;
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
        if (index < 0 || index >= getTabCount()) return;
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
        if (index < 0 || index >= getTabCount()) {
            activeSession = null;
            return;
        }
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
            sessions.remove(previousKey, session);
        }
        componentSessions.put(session.getView(), session);
        if (file != null) {
            EditorSession previous = sessions.put(file, session);
            if (previous != null && previous != session) {
                removeSession(previous);
            }
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
        componentSessions.remove(session.getView());
        if (key != null) {
            sessions.remove(key, session);
        } else {
            sessions.values().removeIf(candidate -> candidate == session);
        }
        boolean wasSelected = session.getView() == getSelectedComponent();
        if (activeSession == session) {
            activeSession = null;
        }
        if (autoSaveManager != null) {
            autoSaveManager.unregister(session.getBuffer().getDocument());
        }
        session.dispose();
        super.remove(session.getView());
        if (wasSelected) {
            updateActiveSessionFromSelection();
        }
    }

    private void wireAutoSave(EditorSession session) {
        if (session == null || autoSaveManager == null) return;
        EditorDocument document = session.getBuffer().getDocument();
        autoSaveManager.register(document);
        session.getView().getTextPane().addFocusListener(new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                AutoSaveManager manager = autoSaveManager;
                if (manager != null) {
                    manager.saveNow(document);
                }
            }
        });
    }

    private void refreshTitle(EditorSession session) {
        if (session == null) return;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> refreshTitle(session));
            return;
        }
        int index = indexOfComponent(session.getView());
        if (index < 0) return;
        File file = sessionKeys.get(session);
        String name = file != null ? file.getName() : "Untitled";
        String title = session.getBuffer().getDocument().isDirty() ? name + " *" : name;
        setTitleAt(index, title);
    }

    private EditorSession findSession(Component component) {
        return component != null ? componentSessions.get(component) : null;
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
