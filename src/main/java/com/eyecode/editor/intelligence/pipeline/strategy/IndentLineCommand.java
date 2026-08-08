package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;

/**
 * Replaces the leading whitespace of the line starting at {@code lineStartOffset}
 * with the given indentation. Only the whitespace prefix is touched — the rest
 * of the line and the caret column stay as they are.
 */
final class IndentLineCommand implements EditorCommand {

    private final int lineStartOffset;
    private final String indentation;

    IndentLineCommand(int lineStartOffset, String indentation) {
        this.lineStartOffset = lineStartOffset;
        this.indentation = indentation == null ? "" : indentation;
    }

    @Override
    public String name() {
        return "indent-line";
    }

    @Override
    public void execute(EditorCommandContext context) {
        String text = context.snapshot().getText();
        int wsEnd = leadingWhitespaceEnd(text, lineStartOffset);
        context.replaceText(lineStartOffset, wsEnd, indentation);
    }

    @Override
    public void undo(EditorCommandContext context) {
    }

    static int leadingWhitespaceEnd(String text, int from) {
        int i = Math.max(0, Math.min(from, text.length()));
        while (i < text.length() && (text.charAt(i) == ' ' || text.charAt(i) == '\t')) {
            i++;
        }
        return i;
    }
}
