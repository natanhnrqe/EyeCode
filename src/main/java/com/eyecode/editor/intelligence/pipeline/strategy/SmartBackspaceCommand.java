package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Smart Backspace dedent: removes leading whitespace back to the previous
 * indentation boundary. A single leading tab counts as one full unit, while
 * misaligned spaces remove only the distance needed to reach that boundary.
 * <p>
 * The strategy guarantees the deleted region is pure whitespace, so the delete
 * stays inside the single pipeline transaction and undoes as one step.
 */
final class SmartBackspaceCommand implements EditorCommand {

    private final int caretOffset;
    private final int indentSize;

    SmartBackspaceCommand(int caretOffset, int indentSize) {
        this.caretOffset = caretOffset;
        this.indentSize = indentSize;
    }

    @Override
    public String name() {
        return "smart-backspace";
    }

    @Override
    public void execute(EditorCommandContext context) {
        String text = context.snapshot().getText();
        int caret = Math.max(0, Math.min(caretOffset, text.length()));
        int removed = removedWhitespace(text, caret, indentSize);
        if (removed > 0) {
            context.deleteText(caret - removed, caret);
        }
        context.moveCaret(SmartEditPositions.positionOf(context.snapshot(), caret - removed));
    }

    @Override
    public void undo(EditorCommandContext context) {
    }

    static int removedWhitespace(String text, int caret, int indentSize) {
        if (caret <= 0 || text == null || text.isEmpty()) {
            return 0;
        }
        int lineStart = caret;
        while (lineStart > 0 && text.charAt(lineStart - 1) != '\n' && text.charAt(lineStart - 1) != '\r') {
            lineStart--;
        }

        int size = Math.max(1, indentSize);
        int visualColumn = 0;
        for (int index = lineStart; index < caret; index++) {
            char current = text.charAt(index);
            visualColumn = current == '\t'
                    ? visualColumn + size - (visualColumn % size)
                    : visualColumn + 1;
        }

        int targetColumn = ((Math.max(1, visualColumn) - 1) / size) * size;
        visualColumn = 0;
        for (int index = lineStart; index < caret; index++) {
            char current = text.charAt(index);
            int nextColumn = current == '\t'
                    ? visualColumn + size - (visualColumn % size)
                    : visualColumn + 1;
            if (nextColumn > targetColumn) {
                return caret - index;
            }
            visualColumn = nextColumn;
        }
        return 0;
    }
}
