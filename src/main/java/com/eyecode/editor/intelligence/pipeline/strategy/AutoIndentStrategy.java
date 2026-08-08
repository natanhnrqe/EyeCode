package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.indent.IndentPolicy;
import com.eyecode.editor.intelligence.indent.JavaIndentPolicy;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;

import java.util.Optional;

/**
 * Handles plain Enter key presses by inserting a newline plus the indentation
 * computed by the {@link IndentPolicy} for the caret's line.
 * <p>
 * Runs below {@link SmartEnterStrategy}: smart-enter claims the brace-aware
 * cases (same-line {@code {}}, closing braces, active selections) at {@code HIGH},
 * and this strategy provides the general auto-indent fallback for everything
 * else. Only plain, unmodified Enter key presses with a collapsed caret are
 * claimed; shortcut combinations (CONTROL/ALT/META) keep their native meaning.
 */
public final class AutoIndentStrategy implements SmartEditStrategy {

    private final IndentPolicy policy;

    public AutoIndentStrategy() {
        this(JavaIndentPolicy.INSTANCE);
    }

    public AutoIndentStrategy(IndentPolicy policy) {
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
        return SmartEditInput.isPlainKeyPressed(event, "ENTER")
                && (event.selection() == null || event.selection().isEmpty());
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        int offset = event.offset();
        DocumentSnapshot snapshot = context.snapshot();
        int line = snapshot.lineMap().lineOfOffset(offset);
        int level = policy.nextLineIndentLevel(snapshot, line);
        return Optional.of(new InsertNewlineCommand(offset, event.selection(), policy.indentationFor(level)));
    }
}
