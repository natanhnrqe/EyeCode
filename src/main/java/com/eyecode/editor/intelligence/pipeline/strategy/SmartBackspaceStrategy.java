package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.indent.IndentPolicy;
import com.eyecode.editor.intelligence.indent.JavaIndentPolicy;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;

import java.util.Optional;

/**
 * Smart Backspace: when the caret sits inside the leading whitespace of a line,
 * one indentation unit is removed through the {@link IndentPolicy}'s indent
 * size instead of deleting a single character. Lines such as
 * {@code "        |"} become {@code "    |"} in one step.
 * <p>
 * The strategy only claims collapsed carets located in {@code (lineStart,
 * whitespaceEnd]} — a caret on column 0, after the content, on a truly empty
 * line, or with an active selection is left to native Backspace (which keeps
 * deleting the previous character / replacing the selection). The whitespace
 * unit ends at the caret, so the removed region is always pure whitespace.
 */
public final class SmartBackspaceStrategy implements SmartEditStrategy {

    private final IndentPolicy policy;

    public SmartBackspaceStrategy() {
        this(JavaIndentPolicy.INSTANCE);
    }

    public SmartBackspaceStrategy(IndentPolicy policy) {
        if (policy == null) {
            throw new IllegalArgumentException("policy must not be null");
        }
        this.policy = policy;
    }

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.NORMAL;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isPlainKeyPressed(event, "BACKSPACE")
                && (event.selection() == null || event.selection().isEmpty());
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        DocumentSnapshot snapshot = context.snapshot();
        String text = snapshot.getText();
        int offset = Math.min(event.offset(), text.length());
        if (offset <= 0) {
            return Optional.empty();
        }
        LineMap map = snapshot.lineMap();
        int line = map.lineOfOffset(offset);
        int lineStart = map.lineStartOffset(line);
        int whitespaceEnd = IndentLineCommand.leadingWhitespaceEnd(text, lineStart);
        if (offset <= lineStart || offset > whitespaceEnd) {
            return Optional.empty();
        }
        return Optional.of(new SmartBackspaceCommand(offset, policy.indentSize()));
    }
}
