package com.eyecode.editor.v2.integration;

import com.eyecode.editor.EditorTab;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.ui.RichEditorView;
import com.eyecode.filesystem.FileSystemService;

import javax.swing.JTabbedPane;
import java.awt.Component;
import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class EditorHostPanel extends JTabbedPane {

    private final Map<File, EditorSession> sessions = new HashMap<>();
    private final FileSystemService fileSystemService;

    public EditorHostPanel(FileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    public EditorSession openFile(File file) {
        if (file == null) return null;

        EditorSession existing = sessions.get(file);
        if (existing != null) {
            setSelectedComponent(existing.getView());
            return existing;
        }

        String content = "";
        if (file.exists()) {
            try {
                content = fileSystemService.readFile(file.toPath());
            } catch (Exception e) {
                content = "";
            }
        }

        EditorDocument document = new EditorDocument(file.toPath(), content);
        EditorBuffer buffer = new EditorBuffer(document);
        RichEditorView view = new RichEditorView(buffer);
        EditorTab tab = new EditorTab(file);

        EditorSession session = new EditorSession(tab, buffer, view);
        sessions.put(file, session);

        addTab(file.getName(), view);
        setSelectedComponent(view);

        return session;
    }

    public EditorSession newUntitled() {
        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);
        RichEditorView view = new RichEditorView(buffer);
        EditorTab tab = new EditorTab(null);

        EditorSession session = new EditorSession(tab, buffer, view);
        addTab("Untitled", view);
        setSelectedComponent(view);

        return session;
    }

    public void closeSession(File file) {
        EditorSession session = sessions.remove(file);
        if (session != null) {
            remove(session.getView());
        }
    }

    public EditorSession getSession(File file) {
        return sessions.get(file);
    }

    public EditorSession getActiveSession() {
        Component selected = getSelectedComponent();
        if (selected == null) return null;
        for (EditorSession session : sessions.values()) {
            if (session.getView() == selected) return session;
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
            session.getBuffer().getDocument().setSourceFile(file.toPath());
            sessions.put(file, session);
        }
    }

    public void saveActive(File file) {
        EditorSession session = getActiveSession();
        if (session == null) return;
        try {
            fileSystemService.writeFile(file.toPath(), session.getBuffer().getDocument().getText());
            session.getBuffer().getDocument().setSourceFile(file.toPath());
            session.getBuffer().getDocument().markClean();
            sessions.put(file, session);
        } catch (Exception ignored) {
        }
    }

    public void closeActive() {
        EditorSession session = getActiveSession();
        if (session == null) return;
        File file = getActiveFile();
        if (file != null) {
            sessions.remove(file);
        }
        remove(session.getView());
    }
}
