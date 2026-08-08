package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;
import com.eyecode.editor.intelligence.selection.SelectionHistory;

import java.util.Optional;

/**
 * Ctrl+Shift+W selection shrink: restores the selection range recorded before
 * the last expansion, walking the shared {@link SelectionHistory} ladder back
 * ({@code R3 → R2 → R1 → collapsed}).
 * <p>
 * The previous range is always the one stored by {@link ExtendSelectionStrategy}
 * — shrinking never recalculates ranges on its own. When the history is empty
 * or was invalidated by a document change, the strategy yields an empty command
 * and the native behavior prevails. Like expansion, shrinking never touches
 * text and never creates undo entries.
 */
public final class ShrinkSelectionStrategy implements SmartEditStrategy {

    private final SelectionHistory history;

    public ShrinkSelectionStrategy() {
        this(new SelectionHistory());
    }

    public ShrinkSelectionStrategy(SelectionHistory history) {
        this.history = history != null ? history : new SelectionHistory();
    }

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.NORMAL;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isShrinkSelection(event);
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        Optional<TextRange> previous = history.pop(context.snapshot().version());
        if (previous.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new SetSelectionCommand(previous.get()));
    }
}
