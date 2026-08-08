package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Removes one indent unit of leading whitespace from the line starting at
 * {@code lineStartOffset}. A single leading tab counts as one unit; otherwise
 * up to {@code indentSize} leading spaces are removed. The rest of the line is
 * left untouched.
 */
final class DedentLineCommand implements EditorCommand {

    private final int lineStartOffset;
    private final int indentSize;

    DedentLineCommand(int lineStartOffset, int indentSize) {
        this.lineStartOffset = lineStartOffset;
        this.indentSize = indentSize;
    }

    @Override
    public String name() {
        return "dedent-line";
    }

    @Override
    public void execute(EditorCommandContext context) {
        String text = context.snapshot().getText();
        int wsEnd = IndentLineCommand.leadingWhitespaceEnd(text, lineStartOffset);
        int removed = 0;
        if (wsEnd > lineStartOffset) {
            String leading = text.substring(lineStartOffset, wsEnd);
            removed = leading.startsWith("\t") ? 1 : Math.min(indentSize, leading.length());
        }
        if (removed > 0) {
            context.deleteText(lineStartOffset, lineStartOffset + removed);
        }
    }

    @Override
    public void undo(EditorCommandContext context) {
    }
}
