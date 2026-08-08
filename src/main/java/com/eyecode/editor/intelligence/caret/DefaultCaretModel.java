package com.eyecode.editor.intelligence.caret;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;

import java.util.Optional;

/**
 * {@link CaretModel} backed by the {@link EditorBuffer} caret/selection state.
 * <p>
 * The model projects the buffer's line/column positions onto plain character
 * offsets through the document's {@code LineMap} and normalizes every range
 * ({@code start <= end}). It performs no editing of its own: mutations go
 * through the same {@code EditorBuffer} API the rest of the editor uses, so
 * listeners, undo history and the UI sync behave exactly as usual.
 */
public final class DefaultCaretModel implements CaretModel {

    private final EditorBuffer buffer;
    private final EditorDocument document;

    public DefaultCaretModel(EditorBuffer buffer) {
        if (buffer == null) {
            throw new IllegalArgumentException("buffer must not be null");
        }
        this.buffer = buffer;
        this.document = buffer.getDocument();
    }

    @Override
    public int offset() {
        return clamp(document.offsetOf(buffer.getCaret()));
    }

    @Override
    public boolean hasSelection() {
        return !buffer.getSelection().isEmpty();
    }

    @Override
    public int selectionStart() {
        return selection().map(TextRange::startOffset).orElse(offset());
    }

    @Override
    public int selectionEnd() {
        return selection().map(TextRange::endOffset).orElse(offset());
    }

    @Override
    public Optional<TextRange> selection() {
        EditorSelection selection = buffer.getSelection();
        if (selection.isEmpty()) {
            return Optional.empty();
        }
        int start = clamp(document.offsetOf(selection.getStart()));
        int end = clamp(document.offsetOf(selection.getEnd()));
        return Optional.of(new TextRange(Math.min(start, end), Math.max(start, end)));
    }

    @Override
    public void moveTo(int offset) {
        buffer.moveCaret(positionOf(clamp(offset)));
    }

    @Override
    public void moveTo(int offset, boolean keepSelection) {
        EditorPosition position = positionOf(clamp(offset));
        if (keepSelection) {
            buffer.setCaretPosition(position);
        } else {
            buffer.moveCaret(position);
        }
    }

    @Override
    public void setSelection(TextRange selection) {
        if (selection == null) {
            return;
        }
        int start = clamp(selection.startOffset());
        int end = clamp(selection.endOffset());
        EditorPosition startPosition = positionOf(Math.min(start, end));
        EditorPosition endPosition = positionOf(Math.max(start, end));
        buffer.setCaretPosition(endPosition);
        buffer.setSelection(new EditorSelection(startPosition, endPosition));
    }

    @Override
    public void clearSelection() {
        moveTo(offset());
    }

    @Override
    public void selectAll() {
        setSelection(new TextRange(0, documentLength()));
    }

    @Override
    public int documentLength() {
        return document.length();
    }

    private EditorPosition positionOf(int offset) {
        return document.positionOf(offset);
    }

    private int clamp(int offset) {
        return Math.max(0, Math.min(offset, document.length()));
    }
}
