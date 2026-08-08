package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.v2.EditorSelection;

/**
 * Pure selection state change: selects the whole document and places the caret
 * at its end without touching the text and without opening a transaction, so
 * no undo entry is created and no change event is fired.
 */
final class SelectAllCommand implements EditorCommand {

    @Override
    public String name() {
        return "select-all";
    }

    @Override
    public void execute(EditorCommandContext context) {
        DocumentSnapshot snapshot = context.snapshot();
        int length = snapshot.length();
        context.moveCaret(SmartEditPositions.positionOf(snapshot, length));
        context.setSelection(new EditorSelection(
                SmartEditPositions.positionOf(snapshot, 0),
                SmartEditPositions.positionOf(snapshot, length)
        ));
    }

    @Override
    public void undo(EditorCommandContext context) {
    }
}
