package com.eyecode.editor.intelligence.pipeline;

import java.util.Optional;

/**
 * Placeholder strategy that never claims an input.
 * <p>
 * It is registered at the lowest priority so the pipeline is always wired while
 * it validates the dispatch path: with only this strategy registered, every
 * input falls through to the editor's default behavior. Real strategies replace
 * it progressively in later sprints.
 */
public final class PassthroughSmartEditStrategy implements SmartEditStrategy {

    @Override
    public SmartEditPriority priority() {
        return SmartEditPriority.LOW;
    }

    @Override
    public boolean supports(EditorInputEvent event, EditorCommandContext context) {
        return false;
    }

    @Override
    public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
        return Optional.empty();
    }
}
