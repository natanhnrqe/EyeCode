package com.eyecode.editor.intelligence.pipeline;

import java.util.Optional;

/**
 * Pluggable handler consulted by the {@link SmartEditingRegistry} for a given
 * input. Smart editing features (brace completion, auto indent, quote
 * completion, smart enter, ...) are implemented as handlers and register
 * themselves with the registry in later sprints.
 */
public interface SmartEditHandler {

    int priority();

    Optional<EditorCommand> tryHandle(EditorInputEvent event, EditorCommandContext context);
}
