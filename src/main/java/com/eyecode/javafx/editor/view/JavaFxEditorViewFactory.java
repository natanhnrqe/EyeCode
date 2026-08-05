package com.eyecode.javafx.editor.view;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.javafx.editor.JavaFxEditor;
import com.eyecode.javafx.editor.JavaFxEditorController;
import com.eyecode.workbench.editor.EditorView;
import com.eyecode.workbench.editor.EditorViewFactory;

import java.nio.file.Path;

public final class JavaFxEditorViewFactory implements EditorViewFactory {

    @Override
    public EditorView create(EditorBuffer buffer) {
        JavaFxEditor editor = new JavaFxEditor();
        JavaFxEditorController controller = new JavaFxEditorController(editor, buffer);
        controller.loadDocument();
        return new JavaFxEditorView(editor, controller);
    }

    @Override
    public boolean supports(Path file) {
        return true;
    }

    @Override
    public String id() {
        return "javafx-code";
    }
}
