package com.eyecode.editor.v2;

import com.eyecode.editor.v2.command.CommandManager;
import com.eyecode.editor.v2.command.DeleteTextCommand;
import com.eyecode.editor.v2.command.EditCommand;
import com.eyecode.editor.v2.command.InsertTextCommand;
import com.eyecode.editor.v2.command.ReplaceTextCommand;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;

import java.util.ArrayList;
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
    private final CommandManager commandManager;

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
        this.commandManager = new CommandManager();
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

    public boolean canUndo() { return commandManager.canUndo(); }

    public boolean canRedo() { return commandManager.canRedo(); }

    public void undo() {
        commandManager.undo(document);
    }

    public void redo() {
        commandManager.redo(document);
    }

    public void executeCommand(EditCommand command) {
        commandManager.execute(command, document);
    }

    public void insertText(int offset, String text) {
        if (text == null || text.isEmpty()) return;
        executeCommand(new InsertTextCommand(offset, text));
    }

    public void deleteText(int start, int end) {
        if (start >= end) return;
        String removed = document.getText().substring(start, end);
        executeCommand(new DeleteTextCommand(start, removed));
    }

    public void replaceText(String newText) {
        if (newText == null) newText = "";
        if (newText.equals(document.getText())) return;
        executeCommand(new ReplaceTextCommand(document.getText(), newText));
    }

    public void runProgrammaticUpdate(Runnable update) {
        commandManager.runProgrammaticUpdate(update);
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
        commandManager.recordTextChange(oldText, newText);
    }

    public interface CaretChangeListener {
        void onCaretChanged(EditorPosition position);
    }

    public interface SelectionChangeListener {
        void onSelectionChanged(EditorSelection selection);
    }
}
