package com.eyecode.editor.v2.command;

import com.eyecode.editor.v2.EditorDocument;

import java.util.ArrayDeque;
import java.util.Deque;

public final class CommandManager {

    private final Deque<EditCommand> undoStack;
    private final Deque<EditCommand> redoStack;
    private boolean applyingHistory;
    private boolean programmaticUpdate;

    public CommandManager() {
        this.undoStack = new ArrayDeque<>();
        this.redoStack = new ArrayDeque<>();
    }

    public boolean canUndo() { return !undoStack.isEmpty(); }

    public boolean canRedo() { return !redoStack.isEmpty(); }

    public boolean isApplyingHistory() { return applyingHistory; }

    public boolean isProgrammaticUpdate() { return programmaticUpdate; }

    public void execute(EditCommand command, EditorDocument document) {
        if (command == null || applyingHistory || programmaticUpdate) return;
        applyingHistory = true;
        try {
            command.execute(document);
        } finally {
            applyingHistory = false;
        }
        pushState(undoStack, command);
        redoStack.clear();
        validateHistoryState();
    }

    public void undo(EditorDocument document) {
        if (applyingHistory || programmaticUpdate || !canUndo()) return;
        EditCommand command = undoStack.pop();
        pushState(redoStack, command);
        applyHistoryCommand(command, true, document);
        validateHistoryState();
    }

    public void redo(EditorDocument document) {
        if (applyingHistory || programmaticUpdate || !canRedo()) return;
        EditCommand command = redoStack.pop();
        pushState(undoStack, command);
        applyHistoryCommand(command, false, document);
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

    public void recordTextChange(String oldText, String newText) {
        if (applyingHistory || programmaticUpdate || oldText.equals(newText)) return;
        pushState(undoStack, new ReplaceTextCommand(oldText, newText));
        redoStack.clear();
        validateHistoryState();
    }

    private void applyHistoryCommand(EditCommand command, boolean isUndo, EditorDocument document) {
        applyingHistory = true;
        try {
            if (isUndo) {
                command.undo(document);
            } else {
                command.execute(document);
            }
        } finally {
            applyingHistory = false;
        }
    }

    private void pushState(Deque<EditCommand> stack, EditCommand command) {
        if (command == null) return;
        if (stack.isEmpty() || !stack.peek().equals(command)) {
            stack.push(command);
        }
    }

    private void validateHistoryState() {
        assertNoConsecutiveDuplicates(undoStack, "undo");
        assertNoConsecutiveDuplicates(redoStack, "redo");
    }

    private void assertNoConsecutiveDuplicates(Deque<EditCommand> stack, String name) {
        EditCommand previous = null;
        boolean hasPrevious = false;
        for (EditCommand command : stack) {
            if (hasPrevious && previous.equals(command)) {
                throw new IllegalStateException("Duplicate consecutive " + name + " history command");
            }
            previous = command;
            hasPrevious = true;
        }
    }
}
