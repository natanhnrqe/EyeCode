package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Inserts an opening and its matching closing character as a single atomic edit
 * and leaves the caret between them, e.g. {@code (} {@code ->} {@code ()}.
 * <p>
 * The pair is produced through {@link EditorCommandContext} only: the document
 * is mutated through the context transaction and never accessed directly.
 */
final class InsertDelimiterPairCommand implements EditorCommand {

    private final char opening;
    private final char closing;
    private final int offset;

    InsertDelimiterPairCommand(char opening, char closing, int offset) {
        this.opening = opening;
        this.closing = closing;
        this.offset = offset;
    }

    @Override
    public String name() {
        return "insert-delimiter-pair-" + opening;
    }

    @Override
    public void execute(EditorCommandContext context) {
        context.insertText(offset, String.valueOf(opening) + closing);
        context.moveCaret(SmartEditPositions.positionOf(context.snapshot(), offset + 1));
    }

    @Override
    public void undo(EditorCommandContext context) {
        context.deleteText(offset, offset + 2);
    }
}
