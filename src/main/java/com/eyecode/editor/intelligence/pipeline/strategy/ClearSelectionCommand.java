package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Pure caret movement: collapses the selection at {@code caretOffset} without
 * touching the document text and without opening a transaction, so no undo
 * entry is created and no change event is fired. The target offset is clamped
 * to the snapshot length and converted through its {@code LineMap}.
 */
final class ClearSelectionCommand implements EditorCommand {

    private final int caretOffset;

    ClearSelectionCommand(int caretOffset) {
        this.caretOffset = caretOffset;
    }

    @Override
    public String name() {
        return "clear-selection";
    }

    @Override
    public void execute(EditorCommandContext context) {
        context.moveCaret(SmartEditPositions.positionOf(context.snapshot(), caretOffset));
    }

    @Override
    public void undo(EditorCommandContext context) {
    }
}
