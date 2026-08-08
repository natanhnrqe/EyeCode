package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.intelligence.pipeline.EditorCommand;
import com.eyecode.editor.intelligence.pipeline.EditorCommandContext;
import com.eyecode.editor.intelligence.pipeline.EditorInputEvent;
import com.eyecode.editor.intelligence.pipeline.SmartEditPriority;
import com.eyecode.editor.intelligence.pipeline.SmartEditStrategy;

import java.util.Optional;

/**
 * Claims the quote characters {@code "} and {@code '}.
 * <p>
 * Decision order: active selection wraps the selection; an already present same
 * quote to the right of the caret triggers skip-over (defensive duplicate of
 * {@link ClosingDelimiterStrategy} so the strategy is self-sufficient); any
 * other position inserts the quote pair with the caret between both quotes.
 * <p>
 * Only the immediate textual state is inspected — no attempt is made to decide
 * whether the caret is semantically inside a string.
 */
public final class QuoteCompletionStrategy implements SmartEditStrategy {

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.NORMAL;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return SmartEditInput.isPlainCharacterTyped(event)
                && Delimiters.isQuote(event.character());
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        char quote = event.character();
        TextRange selection = event.selection();
        if (selection != null && !selection.isEmpty()) {
            return Optional.of(new WrapSelectionWithDelimiterCommand(
                    quote, quote, selection.startOffset(), selection.endOffset()));
        }
        int offset = event.offset();
        String text = context.snapshot().getText();
        if (offset < text.length() && text.charAt(offset) == quote) {
            return Optional.of(new SkipOverDelimiterCommand(offset));
        }
        return Optional.of(new InsertDelimiterPairCommand(quote, quote, offset));
    }
}
