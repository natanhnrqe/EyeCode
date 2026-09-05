package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.FileClosedEvent;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EditorManagerFileOperationTest {

    @Test
    void deletingCleanOpenFileRemovesItAndClosesItsSession() throws Exception {
        Path root = Files.createTempDirectory("eyecode-manager-delete");
        Path file = Files.writeString(root.resolve("Clean.java"), "class Clean {}\n");
        EventBus eventBus = new EventBus();
        List<Path> closed = new ArrayList<>();
        eventBus.subscribe(FileClosedEvent.class, event -> closed.add(event.getFile().toPath()));
        EditorManager manager = new EditorManager(eventBus, new DefaultFileSystemService(), factory());
        EditorSession session = manager.openDocument(file);

        assertTrue(manager.deletePath(ProjectModel.fromDirectory(root.toFile()), file));

        assertFalse(Files.exists(file));
        assertFalse(manager.getSession(session.getSessionId()).isPresent());
        assertNull(manager.getCurrentSession());
        assertEquals(List.of(file), closed);
        manager.shutdownAutosave();
    }

    @Test
    void openSessionFollowsRenameAndOldAutosaveCannotRecreatePath() throws Exception {
        Path root = Files.createTempDirectory("eyecode-manager-rename");
        Path oldPath = Files.writeString(root.resolve("Person.java"), "class Person {}\n");
        EditorManager manager = new EditorManager(new EventBus(), new DefaultFileSystemService(), factory());
        EditorSession session = manager.openDocument(oldPath);
        String sessionId = session.getSessionId();
        String documentId = session.getDocumentId();
        manager.getBuffer(session.getSessionId()).orElseThrow().replaceText("class Person { int value; }\n");

        assertTrue(manager.renamePath(ProjectModel.fromDirectory(root.toFile()), oldPath, "User.java"));
        Path newPath = root.resolve("User.java");
        assertEquals(newPath.toAbsolutePath().normalize(), session.getFile());
        assertEquals(sessionId, session.getSessionId());
        assertEquals(documentId, session.getDocumentId());
        assertSame(session, manager.getSession(sessionId).orElseThrow());
        assertSame(session, manager.getCurrentSession());
        assertEquals(newPath.toAbsolutePath().normalize(),
                manager.getBuffer(sessionId).orElseThrow().getDocument().getSourceFile());
        assertEquals(1, manager.getSessions().size());
        assertFalse(Files.exists(oldPath));
        Thread.sleep(900);
        assertFalse(Files.exists(oldPath));
        assertTrue(Files.readString(newPath).contains("class User"));
        manager.closeAllSessions();
        manager.shutdownAutosave();
    }

    private static EditorViewFactory factory() {
        return new EditorViewFactory() {
            @Override public EditorView create(EditorBuffer buffer) { return new EditorView() {
                @Override public Object getNativeView() { return null; }
                @Override public void refreshFromDocument() { }
                @Override public void dispose() { }
            }; }
            @Override public boolean supports(Path path) { return true; }
            @Override public String id() { return "test"; }
        };
    }
}
