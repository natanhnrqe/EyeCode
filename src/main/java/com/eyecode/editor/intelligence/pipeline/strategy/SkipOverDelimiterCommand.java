package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Moves the caret one position forward over an already present closing
 * delimiter instead of inserting a second one (skip-over).
 * <p>
 * Pure caret move: no text mutation and therefore no transaction and no undo
 * entry.
 */
final class SkipOverDelimiterCommand implements EditorCommand {

    private final int offset;

    SkipOverDelimiterCommand(int offset) {
        this.offset = offset;
    }

    @Override
    public String name() {
        return "skip-over-delimiter";
    }

    @Override
    public void execute(EditorCommandContext context) {
        context.moveCaret(SmartEditPositions.positionOf(context.snapshot(), offset + 1));
    }

    @Override
    public void undo(EditorCommandContext context) {
    }
}
