package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorManagerAutosaveTest {
    @Test
    void flushPersistsJavaFxManagedFileSession() throws Exception {
        Path file = Files.createTempFile("eyecode-autosave", ".java");
        Files.writeString(file, "OLD");
        EditorViewFactory factory = new EditorViewFactory() {
            @Override public EditorView create(EditorBuffer buffer) { return new EditorView() {
                @Override public Object getNativeView() { return null; }
                @Override public void refreshFromDocument() { }
                @Override public void dispose() { }
            }; }
            @Override public boolean supports(Path path) { return true; }
            @Override public String id() { return "test"; }
        };
        EditorManager manager = new EditorManager(new EventBus(), new DefaultFileSystemService(), factory);
        EditorSession session = manager.openDocument(file);
        manager.getBuffer(session.getSessionId()).orElseThrow().replaceText("NEW");

        assertTrue(manager.flushAutosave());
        assertEquals("NEW", Files.readString(file));
        manager.closeAllSessions();
        manager.shutdownAutosave();
    }
}
