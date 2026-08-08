package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Applies the pre-computed smart-enter edit: replaces the active selection (or
 * inserts at the caret offset) with the given text and moves the caret to
 * {@code caretOffset}.
 * <p>
 * All layout decisions (which text to insert, where the caret lands) are made
 * by {@link SmartEnterStrategy} against the snapshot; this command only touches
 * the document through the context, inside the pipeline's single transaction.
 */
final class SmartEnterCommand implements EditorCommand {

    private final int offset;
    private final TextRange selection;
    private final String inserted;
    private final int caretOffset;

    SmartEnterCommand(int offset, TextRange selection, String inserted, int caretOffset) {
        this.offset = offset;
        this.selection = selection;
        this.inserted = inserted == null ? "" : inserted;
        this.caretOffset = caretOffset;
    }

    @Override
    public String name() {
        return "smart-enter";
    }

    @Override
    public void execute(EditorCommandContext context) {
        if (selection != null && !selection.isEmpty()) {
            context.replaceText(selection.startOffset(), selection.endOffset(), inserted);
        } else if (!inserted.isEmpty()) {
            context.insertText(offset, inserted);
        }
        context.moveCaret(SmartEditPositions.positionOf(context.snapshot(), caretOffset));
    }

    @Override
    public void undo(EditorCommandContext context) {
    }
}
