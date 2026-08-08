package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Pure caret movement: moves the caret to {@code offset} without touching the
 * document text and without opening a transaction, so no undo entry is created
 * and no change event is fired. The target offset is clamped to the snapshot
 * length and converted through the snapshot's {@code LineMap}.
 */
final class MoveCaretToOffsetCommand implements EditorCommand {

    private final int offset;

    MoveCaretToOffsetCommand(int offset) {
        this.offset = offset;
    }

    @Override
    public String name() {
        return "move-caret-to-offset";
    }

    @Override
    public void execute(EditorCommandContext context) {
        context.moveCaret(SmartEditPositions.positionOf(context.snapshot(), offset));
    }

    @Override
    public void undo(EditorCommandContext context) {
    }
}
