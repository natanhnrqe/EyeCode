package com.eyecode.javafx.ui.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.javafx.editor.JavaFxEditor;
import com.eyecode.javafx.editor.JavaFxEditorController;
import com.eyecode.javafx.learning.JavaFxLearningWorkspace;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.language.documentation.JdkSourceDeclarationLocator;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class JavaFxJdkSourceTab extends VBox {

    private final JdkSourceTarget target;
    private final JavaFxEditor editor;
    private final JavaFxEditorController controller;
    private final JavaFxLearningWorkspace learningWorkspace;
    private final JdkSourceDeclarationLocator declarationLocator = new JdkSourceDeclarationLocator();
    private final String source;

    public JavaFxJdkSourceTab(JdkSourceTarget target, String source) {
        this.target = target;
        this.source = source == null ? "" : source;
        this.learningWorkspace = new JavaFxLearningWorkspace();
        EditorBuffer buffer = new EditorBuffer(new EditorDocument(null, this.source));
        this.editor = new JavaFxEditor(buffer);
        this.controller = new JavaFxEditorController(editor, buffer, learningWorkspace);
        controller.loadDocument();
        editor.setReadOnly(true);
        reveal(target);
        getStyleClass().add("jdk-source-tab");
        VBox.setVgrow(editor, Priority.ALWAYS);
        getChildren().add(editor);
    }

    public JdkSourceTarget target() {
        return target;
    }

    public JavaFxEditor editor() {
        return editor;
    }

    public void reveal(JdkSourceTarget target) {
        editor.revealOffset(declarationLocator.find(source, target));
    }

    public void dispose() {
        controller.dispose();
        learningWorkspace.dispose();
    }
}
