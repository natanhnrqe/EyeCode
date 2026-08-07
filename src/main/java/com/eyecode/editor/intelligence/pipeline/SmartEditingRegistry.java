package com.eyecode.editor.intelligence.pipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Priority-ordered registry of smart editing handlers.
 * <p>
 * Handlers with a lower {@link SmartEditHandler#priority()} value are consulted
 * first. The first handler that claims an input wins; if none does, the caller
 * falls back to plain editing.
 */
public final class SmartEditingRegistry {

    private final List<SmartEditHandler> handlers = new ArrayList<>();

    public void register(SmartEditHandler handler) {
        if (handler == null || handlers.contains(handler)) return;
        handlers.add(handler);
        handlers.sort(Comparator.comparingInt(SmartEditHandler::priority));
    }

    public void unregister(SmartEditHandler handler) {
        handlers.remove(handler);
    }

    public void clear() {
        handlers.clear();
    }

    public List<SmartEditHandler> handlers() {
        return List.copyOf(handlers);
    }

    public Optional<EditorCommand> handle(EditorInputEvent event, EditorCommandContext context) {
        if (event == null || context == null) return Optional.empty();
        for (SmartEditHandler handler : handlers) {
            Optional<EditorCommand> command = handler.tryHandle(event, context);
            if (command.isPresent()) {
                return command;
            }
        }
        return Optional.empty();
    }
}
