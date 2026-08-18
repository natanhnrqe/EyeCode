package com.eyecode.javafx.editor.view;

import com.eyecode.javafx.editor.JavaFxEditor;
import com.eyecode.javafx.editor.JavaFxEditorController;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorView;
import javafx.scene.Node;

public final class JavaFxEditorView implements EditorView {

    private final JavaFxEditor editor;
    private final JavaFxEditorController controller;

    public JavaFxEditorView(JavaFxEditor editor, JavaFxEditorController controller) {
        this.editor = editor;
        this.controller = controller;
    }

    public Node getNode() {
        return editor;
    }

    public JavaFxEditor getEditor() {
        return editor;
    }

    public JavaFxEditorController getController() {
        return controller;
    }

    @Override
    public Object getNativeView() {
        return editor;
    }

    @Override
    public void refreshFromDocument() {
        controller.loadDocument();
    }

    @Override
    public void bindNavigation(EditorManager manager, String sessionId) {
        controller.bindNavigation(manager, sessionId);
    }

    @Override
    public void dispose() {
        controller.dispose();
    }
}
