package com.eyecode.editor.v2;

import java.util.ArrayList;
import java.util.List;

public final class EditorBuffer {

    private final EditorDocument document;
    private EditorPosition caret;
    private EditorSelection selection;
    private final List<CaretChangeListener> caretListeners;
    private final List<SelectionChangeListener> selectionListeners;

    public EditorBuffer(EditorDocument document) {
        this.document = document;
        this.caret = new EditorPosition(0, 0);
        this.selection = new EditorSelection(caret, caret);
        this.caretListeners = new ArrayList<>();
        this.selectionListeners = new ArrayList<>();
    }

    public void moveCaret(EditorPosition position) {
        setCaretPosition(position);
        setSelection(new EditorSelection(position, position));
    }

    public void setCaretPosition(EditorPosition position) {
        if (position == null) return;
        this.caret = position;
        for (CaretChangeListener listener : List.copyOf(caretListeners)) {
            listener.onCaretChanged(position);
        }
    }

    public void setSelection(EditorSelection selection) {
        if (selection == null) return;
        this.selection = selection;
        for (SelectionChangeListener listener : List.copyOf(selectionListeners)) {
            listener.onSelectionChanged(selection);
        }
    }

    public EditorDocument getDocument() {
        return document;
    }

    public EditorPosition getCaret() { return caret; }

    public EditorSelection getSelection() { return selection; }

    public void addCaretChangeListener(CaretChangeListener listener) {
        if (listener != null && !caretListeners.contains(listener)) {
            caretListeners.add(listener);
        }
    }

    public void removeCaretChangeListener(CaretChangeListener listener) {
        caretListeners.remove(listener);
    }

    public void addSelectionChangeListener(SelectionChangeListener listener) {
        if (listener != null && !selectionListeners.contains(listener)) {
            selectionListeners.add(listener);
        }
    }

    public void removeSelectionChangeListener(SelectionChangeListener listener) {
        selectionListeners.remove(listener);
    }

    public interface CaretChangeListener {
        void onCaretChanged(EditorPosition position);
    }

    public interface SelectionChangeListener {
        void onSelectionChanged(EditorSelection selection);
    }
}
