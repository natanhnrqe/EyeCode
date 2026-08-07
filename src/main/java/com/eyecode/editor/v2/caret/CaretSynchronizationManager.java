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
    private boolean internalUpdate;
    private boolean refreshing;

    public CaretSynchronizationManager(JTextPane textPane, EditorBuffer buffer) {
        this.textPane = textPane;
        this.buffer = buffer;
        this.caretListener = this::syncFromSwing;
        this.bufferCaretListener = this::syncCaretToSwing;
        if (SYNC_ENABLED) {
            this.textPane.addCaretListener(caretListener);
            this.buffer.addCaretChangeListener(bufferCaretListener);
        }
    }

    public void dispose() {
        if (SYNC_ENABLED) {
            textPane.removeCaretListener(caretListener);
            buffer.removeCaretChangeListener(bufferCaretListener);
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

    private EditorPosition toPosition(int offset) {
        return buffer.getDocument().positionOf(offset);
    }

    private int toOffset(EditorPosition position) {
        return buffer.getDocument().offsetOf(position);
    }
}
