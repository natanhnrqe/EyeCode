package com.eyecode.editor.v2.caret;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;

import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;

public final class CaretSynchronizationManager {

    private final JTextPane textPane;
    private final EditorBuffer buffer;
    private final CaretListener caretListener;
    private final EditorBuffer.CaretChangeListener bufferCaretListener;
    private boolean internalUpdate;
    private boolean refreshing;

    public CaretSynchronizationManager(JTextPane textPane, EditorBuffer buffer) {
        this.textPane = textPane;
        this.buffer = buffer;
        this.caretListener = this::syncFromSwing;
        this.bufferCaretListener = this::syncCaretToSwing;
        this.textPane.addCaretListener(caretListener);
        this.buffer.addCaretChangeListener(bufferCaretListener);

    }

    public void dispose() {
        textPane.removeCaretListener(caretListener);
        buffer.removeCaretChangeListener(bufferCaretListener);
    }

    public void setRefreshing(boolean refreshing) {
        this.refreshing = refreshing;
    }

    private void syncFromSwing(CaretEvent event) {
        if (internalUpdate || refreshing) return;

        int caretOffset = event.getDot();
        int selectionStart = Math.min(event.getDot(), event.getMark());
        int selectionEnd = Math.max(event.getDot(), event.getMark());

        buffer.setCaretPosition(toPosition(caretOffset));
        buffer.setSelection(new EditorSelection(toPosition(selectionStart), toPosition(selectionEnd)));
    }

    private void syncCaretToSwing(EditorPosition position) {
        if (internalUpdate) return;

        internalUpdate = true;
        try {
            int offset = toOffset(position);
            if (textPane.getCaretPosition() != offset) {
                textPane.setCaretPosition(offset);
            }
        } finally {
            internalUpdate = false;
        }
    }

    private EditorPosition toPosition(int offset) {
        Document document = textPane.getDocument();
        Element root = document.getDefaultRootElement();
        int safeOffset = Math.max(0, Math.min(offset, document.getLength()));
        int line = root.getElementIndex(safeOffset);
        Element lineElement = root.getElement(line);
        int column = safeOffset - lineElement.getStartOffset();
        return new EditorPosition(line, column);
    }

    private int toOffset(EditorPosition position) {
        Document document = textPane.getDocument();
        Element root = document.getDefaultRootElement();
        int line = Math.max(0, Math.min(position.line(), root.getElementCount() - 1));
        Element lineElement = root.getElement(line);
        int lineLength = lineElement.getEndOffset() - lineElement.getStartOffset();
        int column = Math.max(0, Math.min(position.column(), lineLength));
        int offset = lineElement.getStartOffset() + column;
        return Math.max(0, Math.min(offset, document.getLength()));
    }
}
