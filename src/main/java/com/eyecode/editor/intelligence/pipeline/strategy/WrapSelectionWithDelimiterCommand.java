package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;

/**
 * Wraps the selected text in an opening/closing delimiter pair as one atomic
 * edit, e.g. selection {@code foo} plus {@code (} {@code ->} {@code (foo)}.
 * <p>
 * After the wrap the selection is restored over the inner content so the user
 * can keep editing it. Both inserts go through the context transaction, which
 * makes the whole wrap a single undoable operation.
 */
final class WrapSelectionWithDelimiterCommand implements EditorCommand {

    private final char opening;
    private final char closing;
    private final int selectionStart;
    private final int selectionEnd;

    WrapSelectionWithDelimiterCommand(char opening, char closing, int selectionStart, int selectionEnd) {
        this.opening = opening;
        this.closing = closing;
        this.selectionStart = selectionStart;
        this.selectionEnd = selectionEnd;
    }

    @Override
    public String name() {
        return "wrap-selection-with-" + opening;
    }

    @Override
    public void execute(EditorCommandContext context) {
        context.insertText(selectionStart, String.valueOf(opening));
        context.insertText(selectionEnd + 1, String.valueOf(closing));
        int innerStart = selectionStart + 1;
        int innerEnd = selectionEnd + 1;
        EditorPosition start = SmartEditPositions.positionOf(context.snapshot(), innerStart);
        EditorPosition end = SmartEditPositions.positionOf(context.snapshot(), innerEnd);
        context.moveCaret(end);
        context.setSelection(new EditorSelection(start, end));
    }

    @Override
    public void undo(EditorCommandContext context) {
        context.deleteText(selectionEnd + 1, selectionEnd + 2);
        context.deleteText(selectionStart, selectionStart + 1);
    }
}
