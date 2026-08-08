package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Inserts a newline followed by the given indentation at the caret (or in place
 * of the active selection) and leaves the caret at the end of the inserted
 * indentation. The single atomic edit is committed by the pipeline, so undo
 * restores the previous state in one step.
 */
final class InsertNewlineCommand implements EditorCommand {

    private final int offset;
    private final TextRange selection;
    private final String indentation;

    InsertNewlineCommand(int offset, TextRange selection, String indentation) {
        this.offset = offset;
        this.selection = selection;
        this.indentation = indentation;
    }

    @Override
    public String name() {
        return "insert-newline";
    }

    @Override
    public void execute(EditorCommandContext context) {
        String insertion = "\n" + indentation;
        int caretOffset;
        if (selection != null && !selection.isEmpty()) {
            context.replaceText(selection.startOffset(), selection.endOffset(), insertion);
            caretOffset = selection.startOffset() + insertion.length();
        } else {
            context.insertText(offset, insertion);
            caretOffset = offset + insertion.length();
        }
        context.moveCaret(SmartEditPositions.positionOf(context.snapshot(), caretOffset));
    }

    @Override
    public void undo(EditorCommandContext context) {
    }
}
