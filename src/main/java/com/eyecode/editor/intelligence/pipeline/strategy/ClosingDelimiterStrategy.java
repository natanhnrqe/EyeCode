package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;

import java.util.Optional;

/**
 * Claims the closing delimiters {@code )}, {@code ]}, {@code }}, {@code "} and
 * {@code '} at the highest priority.
 * <p>
 * Its only job is skip-over: when the character immediately to the right of the
 * caret is exactly the typed closing delimiter, the caret moves one position
 * forward instead of inserting a duplicate. Whenever that condition does not
 * hold the strategy yields no command, letting lower-priority strategies (quote
 * completion) or the native editor handle the input.
 */
public final class ClosingDelimiterStrategy implements SmartEditStrategy {

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.HIGH;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isPlainCharacterTyped(event)
                && Delimiters.isClosing(event.character());
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        TextRange selection = event.selection();
        if (selection != null && !selection.isEmpty()) {
            return Optional.empty();
        }
        int offset = event.offset();
        String text = context.snapshot().getText();
        if (offset < text.length() && text.charAt(offset) == event.character()) {
            return Optional.of(new SkipOverDelimiterCommand(offset));
        }
        return Optional.empty();
    }
}
