package com.eyecode.javafx.monaco;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.eventbus.EventBus;
import com.eyecode.filesystem.DefaultFileSystemService;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorView;
import com.eyecode.workbench.editor.EditorViewFactory;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonacoModelIdTest {
    @Test
    void realSessionPathProducesStableFileUriWithoutCreatingFilesystemArtifacts() {
        Path file = Path.of("C:\\project\\src\\Main.java").toAbsolutePath().normalize();
        EditorManager manager = new EditorManager(new EventBus(), new DefaultFileSystemService(), factory());
        try {
            var session = manager.openDocument(file, "class Main {}");
            String modelId = MonacoModelId.forSession(session);

            assertEquals(file.toUri().toString(), modelId);
            assertFalse(modelId.contains("eyecode-monaco"));
            assertFalse(modelId.contains("AppData\\Local\\Temp"));
        } finally {
            manager.shutdownAutosave();
        }
    }

    @Test
    void encodedWindowsUriMatchesCanonicalSessionPath() {
        Path path = Path.of("C:\\Users\\JoyBoy\\project\\Main.java");

        assertTrue(MonacoModelId.matches(
                "file:///c%3A/Users/JoyBoy/project/Main.java", path));
        assertTrue(MonacoModelId.matches(
                "file:///C:/Users/JoyBoy/project/Main.java", path));
    }

    @Test
    void uriPathDecodingPreservesSpacesPlusAndUnicode() {
        Path path = Path.of("C:\\Users\\JoyBoy\\Meu Projeto\\Mais+Fonte\\ação.java");

        assertTrue(MonacoModelId.matches(
                "file:///c%3A/Users/JoyBoy/Meu%20Projeto/Mais%2BFonte/a%C3%A7%C3%A3o.java", path));
    }

    @Test
    void nonFileModelDoesNotMatchSessionPath() {
        assertFalse(MonacoModelId.matches("eyecode://workspace/Main.java",
                Path.of("C:\\Users\\JoyBoy\\project\\Main.java")));
    }

    private static EditorViewFactory factory() {
        return new EditorViewFactory() {
            @Override public EditorView create(EditorBuffer buffer) {
                return new EditorView() {
                    @Override public Object getNativeView() { return new Object(); }
                    @Override public void refreshFromDocument() { }
                    @Override public void dispose() { }
                };
            }
            @Override public boolean supports(Path file) { return true; }
            @Override public String id() { return "test"; }
        };
    }
}
