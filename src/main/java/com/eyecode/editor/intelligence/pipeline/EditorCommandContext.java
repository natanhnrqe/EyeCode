package com.eyecode.editor.intelligence.pipeline;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.DocumentTransaction;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;

/**
 * Context handed to an {@link EditorCommand} when it executes.
 * <p>
 * Strategies never see the raw {@code EditorDocument} or {@code EditorBuffer}:
 * text is read only through immutable {@link DocumentSnapshot}s and mutated only
 * through the exposed {@link DocumentTransaction} or the basic editing methods.
 * This prevents smart editing from manipulating the document arbitrarily.
 * <p>
 * A context is created per pipeline dispatch. Edits are accumulated into one
 * transaction so a smart edit is atomic; caret and selection changes are
 * buffered and applied only if the command commits.
 */
public final class EditorCommandContext {

    private final EditorBuffer buffer;
    private final EditorPosition caret;
    private final EditorSelection selection;
    private DocumentTransaction transaction;
    private EditorPosition targetCaret;
    private EditorSelection targetSelection;
    private boolean selectionExplicitlySet;

    public EditorCommandContext(EditorBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        this.buffer = buffer;
        this.caret = buffer.getCaret();
        this.selection = buffer.getSelection();
        this.targetCaret = caret;
        this.targetSelection = selection;
        this.selectionExplicitlySet = false;
    }

    public DocumentSnapshot snapshot() {
        return buffer.getDocument().snapshot();
    }

    public EditorPosition caret() {
        return caret;
    }

    public EditorSelection selection() {
        return selection;
    }

    public DocumentTransaction transaction() {
        if (transaction == null) {
            transaction = DocumentTransaction.open(buffer.getDocument(), buffer.getCommandManager());
        }
        return transaction;
    }

    public void insertText(int offset, String text) {
        transaction().insert(offset, text);
    }

    public void deleteText(int start, int end) {
        transaction().delete(start, end);
    }

    public void replaceText(int start, int end, String text) {
        transaction().replace(start, end, text);
    }

    public void moveCaret(EditorPosition position) {
        if (position != null) {
            targetCaret = position;
            if (!selectionExplicitlySet) {
                targetSelection = new EditorSelection(position, position);
            }
        }
    }

    public void setSelection(EditorSelection newSelection) {
        if (newSelection != null) {
            targetSelection = newSelection;
            selectionExplicitlySet = true;
        }
    }

    void commit() {
        if (transaction != null) {
            if (transaction.isActive()) {
                transaction.commit();
            }
            transaction = null;
        }
    }

    void rollback() {
        if (transaction != null) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            transaction = null;
        }
    }

    void applyTargetState() {
        buffer.moveCaret(targetCaret);
        buffer.setSelection(targetSelection);
    }
}
