package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;

import java.util.Optional;

/**
 * Conservative Smart Delete. Sprint 5.1d defines no structural delete case:
 * Delete over plain text, at the end of a line, in an empty document and next
 * to delimiters all keep the native editor behavior, so the strategy always
 * yields no command (the pipeline reports {@code notHandled} and the editor
 * deletes natively).
 * <p>
 * The strategy is registered explicitly so the registry documents the intent:
 * it owns the DELETE key, has no conflict with the delimiter strategies
 * ({@code ClosingDelimiterStrategy} etc. only claim typed delimiters and Enter),
 * and future sprints can add structural cases here without touching the other
 * strategies.
 */
public final class SmartDeleteStrategy implements SmartEditStrategy {

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.NORMAL;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isPlainKeyPressed(event, "DELETE");
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        return Optional.empty();
    }
}
