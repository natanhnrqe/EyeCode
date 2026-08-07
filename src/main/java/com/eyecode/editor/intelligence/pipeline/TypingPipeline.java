package com.eyecode.editor.intelligence.pipeline;

import java.util.Optional;

/**
 * Routes raw editor inputs through smart editing handlers.
 * <p>
 * For an input no handler claims, {@link #route} returns an empty result and
 * the caller applies plain editing semantics. This is the single entry point
 * for the typing pipeline used by all future smart editing features.
 */
public final class TypingPipeline {

    private final SmartEditingRegistry registry;

    public TypingPipeline(SmartEditingRegistry registry) {
        if (registry == null) throw new IllegalArgumentException("registry must not be null");
        this.registry = registry;
    }

    public Optional<EditorCommand> route(EditorInputEvent event, EditorCommandContext context) {
        return registry.handle(event, context);
    }

    public SmartEditingRegistry registry() {
        return registry;
    }
}
