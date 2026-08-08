package com.eyecode.editor.v2.caret;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;

import javax.swing.JTextPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;

public final class CaretSynchronizationManager {

    // TEMP EXPERIMENT FLAG: set false to skip caret→textPane sync
    public static boolean SYNC_ENABLED = true;

    private final JTextPane textPane;
    private final EditorBuffer buffer;
    private final CaretListener caretListener;
    private final EditorBuffer.CaretChangeListener bufferCaretListener;
    private final EditorBuffer.SelectionChangeListener bufferSelectionListener;
    private boolean internalUpdate;
    private boolean refreshing;

    public CaretSynchronizationManager(JTextPane textPane, EditorBuffer buffer) {
        this.textPane = textPane;
        this.buffer = buffer;
        this.caretListener = this::syncFromSwing;
        this.bufferCaretListener = this::syncCaretToSwing;
        this.bufferSelectionListener = this::syncSelectionToSwing;
        if (SYNC_ENABLED) {
            this.textPane.addCaretListener(caretListener);
            this.buffer.addCaretChangeListener(bufferCaretListener);
            this.buffer.addSelectionChangeListener(bufferSelectionListener);
        }
    }

    public void dispose() {
        if (SYNC_ENABLED) {
            textPane.removeCaretListener(caretListener);
            buffer.removeCaretChangeListener(bufferCaretListener);
            buffer.removeSelectionChangeListener(bufferSelectionListener);
        }
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

    private void syncSelectionToSwing(EditorSelection selection) {
        if (internalUpdate) return;

        int start = toOffset(selection.getStart());
        int end = toOffset(selection.getEnd());
        int dot = Math.max(start, end);
        int mark = Math.min(start, end);
        if (textPane.getSelectionStart() == mark && textPane.getSelectionEnd() == dot) {
            return;
        }
        internalUpdate = true;
        try {
            textPane.select(mark, dot);
        } finally {
            internalUpdate = false;
        }
    }

    private EditorPosition toPosition(int offset) {
        return buffer.getDocument().positionOf(offset);
    }

    private int toOffset(EditorPosition position) {
        return buffer.getDocument().offsetOf(position);
    }
}
