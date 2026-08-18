package com.eyecode.javafx.editor;

import com.eyecode.editor.intelligence.caret.CaretModel;
import com.eyecode.editor.intelligence.caret.DefaultCaretModel;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.editor.intelligence.events.DocumentTextChangeEvent;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.language.semantic.DefinitionLocation;
import com.eyecode.workbench.editor.EditorManager;
import org.fxmisc.richtext.CodeArea;
import org.reactfx.Subscription;

import java.util.Optional;

/**
 * Keeps the {@link CodeArea} and the {@link EditorBuffer} model in sync.
 * <p>
 * Normal typing flows codeArea -> document; smart edits and undo/redo flow
 * document -> codeArea. Both directions are guarded against feedback loops.
 * Selection changes produced by smart editing (Ctrl+W expansion) are projected
 * onto the view through the {@link CaretModel}, the offset-based caret +
 * selection abstraction of the Core.
 */
public final class JavaFxEditorController {

    private final JavaFxEditor editor;
    private final EditorBuffer buffer;
    private final HighlightPipeline pipeline;
    private final Subscription changeSubscription;
    private final DocumentChangeListener documentChangeListener;
    private final EditorBuffer.CaretChangeListener caretChangeListener;
    private final EditorBuffer.SelectionChangeListener selectionChangeListener;
    private EditorManager manager;
    private String sessionId;
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
        this.selectionChangeListener = this::syncSelectionToView;
        buffer.getDocument().addDocumentChangeListener(documentChangeListener);
        buffer.addCaretChangeListener(caretChangeListener);
        buffer.addSelectionChangeListener(selectionChangeListener);
        editor.setGoToDefinitionAction(this::goToDefinition);
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

    private void syncSelectionToView(EditorSelection selection) {
        if (syncing) {
            return;
        }
        CaretModel caretModel = new DefaultCaretModel(buffer);
        Optional<TextRange> range = caretModel.selection();
        if (range.isEmpty()) {
            return;
        }
        CodeArea codeArea = editor.getCodeArea();
        syncing = true;
        try {
            codeArea.selectRange(range.get().startOffset(), range.get().endOffset());
            codeArea.requestFollowCaret();
        } finally {
            syncing = false;
        }
    }

    private int offsetOf(EditorPosition position) {
        return buffer.getDocument().offsetOf(position);
    }

    public void bindNavigation(EditorManager manager, String sessionId) {
        this.manager = manager;
        this.sessionId = sessionId;
    }

    public boolean goToDefinition() {
        if (manager == null || sessionId == null) {
            return false;
        }
        int caretOffset = editor.getCodeArea().getCaretPosition();
        Optional<DefinitionLocation> location = manager.resolveDefinition(sessionId, caretOffset);
        if (location.isEmpty()) {
            return false;
        }
        editor.revealOffset(location.get().declarationRange().startOffset());
        return true;
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
        buffer.removeSelectionChangeListener(selectionChangeListener);
    }
}
