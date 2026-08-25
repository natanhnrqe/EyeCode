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
