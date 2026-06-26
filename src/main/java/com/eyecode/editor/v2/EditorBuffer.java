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
    private boolean programmaticUpdate;

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
        if (applyingHistory || programmaticUpdate || !canUndo()) return;
        String currentText = document.getText();
        String previousText = undoStack.pop();
        pushState(redoStack, currentText);
        applyHistoryText(previousText);
        validateHistoryState();
    }

    public void redo() {
        if (applyingHistory || programmaticUpdate || !canRedo()) return;
        String currentText = document.getText();
        String nextText = redoStack.pop();
        pushState(undoStack, currentText);
        applyHistoryText(nextText);
        validateHistoryState();
    }

    public void runProgrammaticUpdate(Runnable update) {
        if (update == null) return;
        boolean wasProgrammaticUpdate = programmaticUpdate;
        programmaticUpdate = true;
        try {
            update.run();
        } finally {
            programmaticUpdate = wasProgrammaticUpdate;
            validateHistoryState();
        }
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
        if (applyingHistory || programmaticUpdate || oldText.equals(newText)) return;
        pushState(undoStack, oldText);
        redoStack.clear();
        validateHistoryState();
    }

    private void applyHistoryText(String text) {
        applyingHistory = true;
        try {
            document.setText(text);
        } finally {
            applyingHistory = false;
        }
    }

    private void pushState(Deque<String> stack, String text) {
        if (text == null) text = "";
        if (stack.isEmpty() || !stack.peek().equals(text)) {
            stack.push(text);
        }
    }

    private void validateHistoryState() {
        assertNoConsecutiveDuplicates(undoStack, "undo");
        assertNoConsecutiveDuplicates(redoStack, "redo");
        if (!undoStack.isEmpty() && undoStack.peek().equals(document.getText())) {
            throw new IllegalStateException("Undo history top matches current document text");
        }
    }

    private void assertNoConsecutiveDuplicates(Deque<String> stack, String name) {
        String previous = null;
        boolean hasPrevious = false;
        for (String state : stack) {
            if (hasPrevious && previous.equals(state)) {
                throw new IllegalStateException("Duplicate consecutive " + name + " history state");
            }
            previous = state;
            hasPrevious = true;
        }
    }

    public interface CaretChangeListener {
        void onCaretChanged(EditorPosition position);
    }

    public interface SelectionChangeListener {
        void onSelectionChanged(EditorSelection selection);
    }
}
