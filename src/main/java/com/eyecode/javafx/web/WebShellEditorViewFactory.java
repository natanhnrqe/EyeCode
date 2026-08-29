package com.eyecode.javafx.web;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorView;
import com.eyecode.workbench.editor.EditorViewFactory;

import java.nio.file.Path;

final class WebShellEditorViewFactory implements EditorViewFactory {
    @Override
    public EditorView create(EditorBuffer buffer) {
        return new EditorView() {
            @Override
            public Object getNativeView() {
                return null;
            }

            @Override
            public void refreshFromDocument() {
            }

            @Override
            public void bindNavigation(EditorManager manager, String sessionId) {
            }

            @Override
            public void dispose() {
            }
        };
    }

    @Override
    public boolean supports(Path file) {
        return true;
    }

    @Override
    public String id() {
        return "web-shell-editor";
    }
}
