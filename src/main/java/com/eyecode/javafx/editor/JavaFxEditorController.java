package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.editor.intelligence.events.DocumentTextChangeEvent;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.Subscription;

/**
 * Keeps the {@link CodeArea} and the {@link EditorBuffer} model in sync.
 * <p>
 * Normal typing flows codeArea -> document; smart edits and undo/redo flow
 * document -> codeArea. Both directions are guarded against feedback loops.
 */
public final class JavaFxEditorController {

    private final JavaFxEditor editor;
    private final EditorBuffer buffer;
    private final HighlightPipeline pipeline;
    private final Subscription changeSubscription;
    private final DocumentChangeListener documentChangeListener;
    private final EditorBuffer.CaretChangeListener caretChangeListener;
    private boolean syncing;

    public JavaFxEditorController(JavaFxEditor editor, EditorBuffer buffer) {
        this.editor = editor;
        this.buffer = buffer;
        this.pipeline = new HighlightPipeline(editor.getCodeArea());

        this.changeSubscription = editor.getCodeArea()
                .multiPlainChanges()
                .subscribe(changes -> refreshFromEditor());

        this.documentChangeListener = this::refreshFromDocument;
        this.caretChangeListener = this::syncCaretToView;
        buffer.getDocument().addDocumentChangeListener(documentChangeListener);
        buffer.addCaretChangeListener(caretChangeListener);
    }

    public void loadDocument() {
        EditorDocument document = buffer.getDocument();
        editor.setText(document.getText());
        pipeline.refresh(document);
    }

    private void refreshFromEditor() {
        if (syncing) {
            return;
        }
        EditorDocument document = buffer.getDocument();
        if (document != null) {
            document.setText(editor.getText());
            pipeline.refresh(document);
        }
    }

    private void refreshFromDocument(DocumentTextChangeEvent event) {
        if (syncing) {
            return;
        }
        EditorDocument document = buffer.getDocument();
        String newText = document.getText();
        CodeArea codeArea = editor.getCodeArea();
        if (!newText.contentEquals(codeArea.getText())) {
            syncing = true;
            try {
                codeArea.replaceText(newText);
                codeArea.moveTo(offsetOf(buffer.getCaret()));
                codeArea.requestFollowCaret();
            } finally {
                syncing = false;
            }
        }
        pipeline.refresh(document);
    }

    private void syncCaretToView(EditorPosition position) {
        if (syncing) {
            return;
        }
        CodeArea codeArea = editor.getCodeArea();
        int offset = offsetOf(position);
        if (codeArea.getCaretPosition() != offset) {
            syncing = true;
            try {
                codeArea.moveTo(offset);
                codeArea.requestFollowCaret();
            } finally {
                syncing = false;
            }
        }
    }

    private int offsetOf(EditorPosition position) {
        return buffer.getDocument().offsetOf(position);
    }

    public void syncToDocument() {
        if (buffer != null && buffer.getDocument() != null) {
            buffer.getDocument().setText(editor.getText());
        }
    }

    public void dispose() {
        changeSubscription.unsubscribe();
        buffer.getDocument().removeDocumentChangeListener(documentChangeListener);
        buffer.removeCaretChangeListener(caretChangeListener);
    }
}
