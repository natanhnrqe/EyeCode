package com.eyecode.javafx.monaco;

import com.eyecode.workbench.editor.EditorSession;

import java.nio.file.Path;

public final class MonacoModelId {
    private MonacoModelId() {
    }

    public static String forSession(EditorSession session) {
        Path file = session.getFile();
        if (file != null) {
            return file.toAbsolutePath().normalize().toUri().toString();
        }
        return "eyecode://workspace/" + session.getDocumentId() + ".java";
    }
}
