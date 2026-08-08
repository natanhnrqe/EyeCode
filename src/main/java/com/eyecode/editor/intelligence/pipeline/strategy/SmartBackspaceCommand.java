package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Smart Backspace dedent: removes exactly the leading whitespace that ends at
 * the caret so the caret returns to the previous indentation unit. A single
 * leading tab counts as one full unit; otherwise up to {@code indentSize}
 * leading spaces are removed (misaligned partial indentation collapses to the
 * previous level, leaving at most {@code indentSize - 1} residual spaces).
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
        if (text.charAt(caret - 1) == '\t') {
            return 1;
        }
        int start = caret;
        while (start > 0 && text.charAt(start - 1) == ' ') {
            start--;
        }
        return Math.min(indentSize, caret - start);
    }
}
