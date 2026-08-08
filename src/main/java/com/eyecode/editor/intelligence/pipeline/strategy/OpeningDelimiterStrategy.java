package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;

import java.util.Optional;

/**
 * Claims the opening delimiters {@code (}, {@code [} and {@code {}.
 * <p>
 * With a collapsed caret it inserts the full pair and leaves the caret between
 * the two characters; with an active selection it wraps the selection.
 */
public final class OpeningDelimiterStrategy implements SmartEditStrategy {

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.NORMAL;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isPlainCharacterTyped(event)
                && Delimiters.isOpening(event.character());
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        char opening = event.character();
        char closing = Delimiters.closingFor(opening);
        TextRange selection = event.selection();
        if (selection != null && !selection.isEmpty()) {
            return Optional.of(new WrapSelectionWithDelimiterCommand(
                    opening, closing, selection.startOffset(), selection.endOffset()));
        }
        return Optional.of(new InsertDelimiterPairCommand(opening, closing, event.offset()));
    }
}
