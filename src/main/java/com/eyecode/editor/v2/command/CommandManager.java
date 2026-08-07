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
        pushState(undoStack, command, true);
        redoStack.clear();
        validateHistoryState();
    }

    public void undo(EditorDocument document) {
        if (applyingHistory || programmaticUpdate || !canUndo()) return;
        EditCommand command = undoStack.pop();
        pushState(redoStack, command, false);
        applyHistoryCommand(command, true, document);
        validateHistoryState();
    }

    public void redo(EditorDocument document) {
        if (applyingHistory || programmaticUpdate || !canRedo()) return;
        EditCommand command = redoStack.pop();
        pushState(undoStack, command, false);
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
        pushState(undoStack, new ReplaceTextCommand(oldText, newText), true);
        redoStack.clear();
        validateHistoryState();
    }

    /**
     * Records a command that was already applied (for example a
     * {@link CompositeEditCommand} committed through a document transaction) as
     * a single undoable unit, without executing it again and without coalescing.
     */
    public void recordGroup(EditCommand group) {
        if (group == null || applyingHistory || programmaticUpdate) return;
        pushState(undoStack, group, false);
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

    private void pushState(Deque<EditCommand> stack, EditCommand command, boolean coalesce) {
        if (command == null) return;
        if (coalesce) {
            EditCommand merged = tryCoalesce(stack.peek(), command);
            if (merged != null) {
                stack.pop();
                stack.push(merged);
                return;
            }
        }
        if (stack.isEmpty() || !stack.peek().equals(command)) {
            stack.push(command);
        }
    }

    private EditCommand tryCoalesce(EditCommand top, EditCommand next) {
        if (top instanceof InsertTextCommand a && next instanceof InsertTextCommand b) {
            if (a.getOffset() + a.getText().length() == b.getOffset()) {
                return new InsertTextCommand(a.getOffset(), a.getText() + b.getText());
            }
            return null;
        }
        if (top instanceof ReplaceTextCommand a && next instanceof ReplaceTextCommand b) {
            if (isAppendOnly(a.getOldText(), a.getNewText())
                    && isAppendOnly(b.getOldText(), b.getNewText())
                    && a.getNewText().equals(b.getOldText())) {
                return new ReplaceTextCommand(a.getOldText(), b.getNewText());
            }
            return null;
        }
        return null;
    }

    private boolean isAppendOnly(String oldText, String newText) {
        return newText.length() > oldText.length() && newText.startsWith(oldText);
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
