package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.project.model.ProjectModel;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class EditorManagerFileOperationTest {

    @Test
    void openSessionFollowsRenameAndOldAutosaveCannotRecreatePath() throws Exception {
        Path root = Files.createTempDirectory("eyecode-manager-rename");
        Path oldPath = Files.writeString(root.resolve("Person.java"), "class Person {}\n");
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
        EditorSession session = manager.openDocument(oldPath);
        manager.getBuffer(session.getSessionId()).orElseThrow().replaceText("class Person { int value; }\n");

        assertTrue(manager.renamePath(ProjectModel.fromDirectory(root.toFile()), oldPath, "User.java"));
        Path newPath = root.resolve("User.java");
        assertEquals(newPath.toAbsolutePath().normalize(), session.getFile());
        assertFalse(Files.exists(oldPath));
        Thread.sleep(900);
        assertFalse(Files.exists(oldPath));
        assertTrue(Files.readString(newPath).contains("class User"));
        manager.closeAllSessions();
        manager.shutdownAutosave();
    }
}
