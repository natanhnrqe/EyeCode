package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;
import com.eyecode.editor.intelligence.selection.JavaSelectionExpander;
import com.eyecode.editor.intelligence.selection.SelectionExpander;
import com.eyecode.editor.intelligence.selection.SelectionHistory;

import java.util.Optional;

/**
 * Ctrl+W selection expansion (VS Code / IntelliJ style).
 * <p>
 * With a collapsed caret the strategy selects the word at the caret (level 1)
 * and records the collapsed state in the shared {@link SelectionHistory}. With
 * an existing selection it infers the current semantic level by matching the
 * selection against the {@link SelectionExpander} results and expands to the
 * next level that actually grows the range. When no further level can grow
 * (whole document, single-word document, no enclosing structure) the strategy
 * yields an empty command and the native behavior prevails.
 * <p>
 * Expansion is a pure selection change: no text is touched, no transaction is
 * opened and no undo entry is created.
 */
public final class ExtendSelectionStrategy implements SmartEditStrategy {

    private final SelectionExpander expander;
    private final SelectionHistory history;

    public ExtendSelectionStrategy() {
        this(new JavaSelectionExpander(), new SelectionHistory());
    }

    public ExtendSelectionStrategy(SelectionExpander expander, SelectionHistory history) {
        this.expander = expander != null ? expander : new JavaSelectionExpander();
        this.history = history != null ? history : new SelectionHistory();
    }

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.NORMAL;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isExtendSelection(event);
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        DocumentSnapshot snapshot = context.snapshot();
        int caret = Math.max(0, Math.min(event.offset(), snapshot.length()));
        Optional<TextRange> current = event.selection() == null || event.selection().isEmpty()
                ? Optional.empty()
                : Optional.of(clampRange(snapshot, event.selection()));
        if (current.isEmpty()) {
            Optional<TextRange> first = expander.expand(snapshot, caret, Optional.empty(), 1);
            if (first.isEmpty()) {
                return Optional.empty();
            }
            history.push(snapshot.version(), 0, new TextRange(caret, caret));
            return Optional.of(new SetSelectionCommand(first.get()));
        }
        int currentLevel = inferLevel(snapshot, caret, current);
        for (int level = 1; level <= expander.maxLevel(); level++) {
            Optional<TextRange> target = expander.expand(snapshot, caret, current, level);
            if (target.isPresent() && grows(target.get(), current.get())) {
                history.push(snapshot.version(), currentLevel, current.get());
                return Optional.of(new SetSelectionCommand(target.get()));
            }
        }
        return Optional.empty();
    }

    private int inferLevel(DocumentSnapshot snapshot, int caret, Optional<TextRange> selection) {
        for (int level = 1; level <= expander.maxLevel(); level++) {
            Optional<TextRange> at = expander.expand(snapshot, caret, selection, level);
            if (at.isPresent() && at.get().equals(selection.get())) {
                return level;
            }
        }
        return 0;
    }

    private static boolean grows(TextRange candidate, TextRange base) {
        return candidate.startOffset() <= base.startOffset()
                && candidate.endOffset() >= base.endOffset()
                && candidate.length() > base.length();
    }

    private static TextRange clampRange(DocumentSnapshot snapshot, TextRange range) {
        int length = snapshot.length();
        return new TextRange(
                Math.min(range.startOffset(), length),
                Math.min(range.endOffset(), length)
        );
    }
}
