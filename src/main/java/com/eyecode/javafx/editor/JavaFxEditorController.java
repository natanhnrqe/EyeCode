package com.eyecode.javafx.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import org.reactfx.Subscription;

public final class JavaFxEditorController {

    private final JavaFxEditor editor;
    private final EditorBuffer buffer;
    private final HighlightPipeline pipeline;
    private final Subscription changeSubscription;

    public JavaFxEditorController(JavaFxEditor editor, EditorBuffer buffer) {
        this.editor = editor;
        this.buffer = buffer;
        this.pipeline = new HighlightPipeline(editor.getCodeArea());

        this.changeSubscription = editor.getCodeArea()
                .multiPlainChanges()
                .subscribe(changes -> refreshFromEditor());
    }

    public void loadDocument() {
        EditorDocument document = buffer.getDocument();
        editor.setText(document.getText());
        pipeline.refresh(document);
    }

    private void refreshFromEditor() {
        EditorDocument document = buffer.getDocument();
        if (document != null) {
            document.setText(editor.getText());
            pipeline.refresh(document);
        }
    }

    public void syncToDocument() {
        if (buffer != null && buffer.getDocument() != null) {
            buffer.getDocument().setText(editor.getText());
        }
    }
}