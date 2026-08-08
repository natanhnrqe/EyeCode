package com.eyecode.editor.intelligence.pipeline;

import java.util.Optional;

/**
 * Resolves an {@link EditorInputEvent} into an {@link EditorCommand} by
 * consulting the {@link SmartEditingRegistry}.
 * <p>
 * Strategies are visited in priority order; the first strategy that both claims
 * the event ({@code supports}) and produces a command wins. If none does, the
 * result is empty and the caller proceeds with plain editing semantics. The
 * dispatcher coordinates only — it knows nothing about Java, JavaFX, RichTextFX,
 * Swing, indentation or autocomplete.
 */
public final class EditorInputDispatcher {

    private final SmartEditingRegistry registry;

    public EditorInputDispatcher(SmartEditingRegistry registry) {
        if (registry == null) {
            throw new IllegalArgumentException("registry must not be null");
        }
        this.registry = registry;
    }

    public Optional<EditorCommand> dispatch(EditorInputEvent event, EditorCommandContext context) {
        if (event == null || context == null) {
            return Optional.empty();
        }
        for (SmartEditStrategy strategy : registry.strategies()) {
            if (strategy.supports(event, context)) {
                Optional<EditorCommand> command = strategy.createCommand(event, context);
                if (command.isPresent()) {
                    return command;
                }
            }
        }
        return Optional.empty();
    }

    public SmartEditingRegistry registry() {
        return registry;
    }
}
