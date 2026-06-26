package com.eyecode.editor.v2;

import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class EditorBuffer {

    private final EditorDocument document;
    private EditorPosition caret;
    private EditorSelection selection;
    private DiagnosticSnapshot diagnostics;
    private LanguageContext languageContext;
    private CompletionSnapshot completionSnapshot;
    private CompletionItem completionSelection;
    private final List<CaretChangeListener> caretListeners;
    private final List<SelectionChangeListener> selectionListeners;
    private final Deque<String> undoStack;
    private final Deque<String> redoStack;
    private boolean applyingHistory;

    public EditorBuffer(EditorDocument document) {
        this.document = document;
        this.caret = new EditorPosition(0, 0);
        this.selection = new EditorSelection(caret, caret);
        this.diagnostics = DiagnosticSnapshot.empty();
        this.completionSnapshot = CompletionSnapshot.empty();
        this.languageContext = new LanguageContext(
                document,
                caret,
                selection,
                new SyntaxSnapshot(List.of()),
                diagnostics
        );
        this.caretListeners = new ArrayList<>();
        this.selectionListeners = new ArrayList<>();
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
        this.document.addTextChangeListener(this::trackTextChange);
    }

    public void moveCaret(EditorPosition position) {
        setCaretPosition(position);
        setSelection(new EditorSelection(position, position));
    }

    public void setCaretPosition(EditorPosition position) {
        if (position == null) return;
        if (position.equals(this.caret)) return;
        this.caret = position;
        for (CaretChangeListener listener : List.copyOf(caretListeners)) {
            listener.onCaretChanged(position);
        }
    }

    public void setSelection(EditorSelection selection) {
        if (selection == null) return;
        if (selection.equals(this.selection)) return;
        this.selection = selection;
        for (SelectionChangeListener listener : List.copyOf(selectionListeners)) {
            listener.onSelectionChanged(selection);
        }
    }

    public void clearListeners() {
        caretListeners.clear();
        selectionListeners.clear();
    }

    public EditorDocument getDocument() {
        return document;
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }

    public boolean canRedo() { return !redoStack.isEmpty(); }

    public void undo() {
        if (!canUndo()) return;
        String currentText = document.getText();
        String previousText = undoStack.pop();
        redoStack.push(currentText);
        applyHistoryText(previousText);
    }

    public void redo() {
        if (!canRedo()) return;
        String currentText = document.getText();
        String nextText = redoStack.pop();
        undoStack.push(currentText);
        applyHistoryText(nextText);
    }

    public EditorPosition getCaret() { return caret; }

    public EditorSelection getSelection() { return selection; }

    public DiagnosticSnapshot getDiagnostics() { return diagnostics; }

    public void setDiagnostics(DiagnosticSnapshot diagnostics) {
        this.diagnostics = diagnostics == null ? DiagnosticSnapshot.empty() : diagnostics;
    }

    public LanguageContext getLanguageContext() { return languageContext; }

    public void setLanguageContext(LanguageContext languageContext) {
        if (languageContext != null) {
            this.languageContext = languageContext;
        }
    }

    public CompletionSnapshot getCompletionSnapshot() { return completionSnapshot; }

    public void setCompletionSnapshot(CompletionSnapshot completionSnapshot) {
        this.completionSnapshot = completionSnapshot == null
                ? CompletionSnapshot.empty()
                : completionSnapshot;
    }

    public CompletionItem getCompletionSelection() { return completionSelection; }

    public void setCompletionSelection(CompletionItem completionSelection) {
        this.completionSelection = completionSelection;
    }

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

    private void trackTextChange(String oldText, String newText) {
        if (applyingHistory || oldText.equals(newText)) return;
        if (undoStack.isEmpty() || !undoStack.peek().equals(oldText)) {
            undoStack.push(oldText);
        }
        redoStack.clear();
    }

    private void applyHistoryText(String text) {
        applyingHistory = true;
        try {
            document.setText(text);
        } finally {
            applyingHistory = false;
        }
    }

    public interface CaretChangeListener {
        void onCaretChanged(EditorPosition position);
    }

    public interface SelectionChangeListener {
        void onSelectionChanged(EditorSelection selection);
    }
}
