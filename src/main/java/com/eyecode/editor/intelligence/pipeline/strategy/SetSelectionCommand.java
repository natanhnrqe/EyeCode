package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.v2.EditorSelection;

/**
 * Pure selection state change: sets the selection to {@code range} and places
 * the caret at its end without touching the document text and without opening
 * a transaction, so no undo entry is created and no change event is fired.
 * The range is clamped to the snapshot and converted through its
 * {@code LineMap}; an empty range collapses the selection.
 */
final class SetSelectionCommand implements EditorCommand {

    private final TextRange range;

    SetSelectionCommand(TextRange range) {
        this.range = range;
    }

    @Override
    public String name() {
        return "set-selection";
    }

    @Override
    public void execute(EditorCommandContext context) {
        DocumentSnapshot snapshot = context.snapshot();
        int length = snapshot.length();
        int start = Math.max(0, Math.min(range.startOffset(), length));
        int end = Math.max(start, Math.min(range.endOffset(), length));
        context.moveCaret(SmartEditPositions.positionOf(snapshot, end));
        context.setSelection(new EditorSelection(
                SmartEditPositions.positionOf(snapshot, start),
                SmartEditPositions.positionOf(snapshot, end)
        ));
    }

    @Override
    public void undo(EditorCommandContext context) {
    }
}
