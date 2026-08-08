package com.eyecode.editor.intelligence.pipeline;

import java.util.Optional;

/**
 * Entry point for the smart editing pipeline.
 * <p>
 * A view hands an {@link EditorInputEvent} to {@link #process}; the pipeline
 * normalizes it, asks the {@link EditorInputDispatcher} to resolve a strategy,
 * executes the resulting {@link EditorCommand} inside a single
 * {@code DocumentTransaction} and reports the outcome.
 * <p>
 * A smart edit is atomic: all edits issued by the command are committed as one
 * undoable unit and exactly one merged document change event reaches the
 * {@code EventBus}. If the command throws, the transaction is rolled back and
 * no partial state remains.
 */
public final class TypingPipeline {

    private final EditorInputDispatcher dispatcher;

    public TypingPipeline(SmartEditingRegistry registry) {
        this(new EditorInputDispatcher(registry));
    }

    public TypingPipeline(EditorInputDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new IllegalArgumentException("dispatcher must not be null");
        }
        this.dispatcher = dispatcher;
    }

    public Optional<EditorCommand> route(EditorInputEvent event, EditorCommandContext context) {
        return dispatcher.dispatch(event, context);
    }

    public SmartEditResult process(EditorInputEvent event, EditorCommandContext context) {
        if (event == null || context == null) {
            return SmartEditResult.notHandled(event);
        }
        Optional<EditorCommand> resolved = dispatcher.dispatch(event, context);
        if (resolved.isEmpty()) {
            return SmartEditResult.notHandled(event);
        }
        EditorCommand command = resolved.get();
        if (!command.canExecute(context)) {
            return SmartEditResult.notHandled(event);
        }
        try {
            command.execute(context);
            context.commit();
            context.applyTargetState();
            return SmartEditResult.handled(event, command);
        } catch (RuntimeException ex) {
            context.rollback();
            return SmartEditResult.failed(event, command, ex);
        }
    }

    public EditorInputDispatcher dispatcher() {
        return dispatcher;
    }
}
