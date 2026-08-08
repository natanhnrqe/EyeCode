package com.eyecode.editor.intelligence.pipeline;

import java.util.Optional;

/**
 * Outcome of a {@link TypingPipeline} dispatch.
 * <p>
 * A result is either {@code notHandled} (no strategy claimed the input, so the
 * editor must proceed with its default behavior), {@code handled} (a smart edit
 * was applied atomically), or {@code failed} (a strategy claimed the input but
 * the command threw; the pipeline rolled back, leaving no partial state).
 */
public final class SmartEditResult {

    private final EditorInputEvent event;
    private final boolean handled;
    private final EditorCommand command;
    private final Throwable failure;

    private SmartEditResult(EditorInputEvent event, boolean handled, EditorCommand command, Throwable failure) {
        this.event = event;
        this.handled = handled;
        this.command = command;
        this.failure = failure;
    }

    public static SmartEditResult notHandled(EditorInputEvent event) {
        return new SmartEditResult(event, false, null, null);
    }

    public static SmartEditResult handled(EditorInputEvent event, EditorCommand command) {
        return new SmartEditResult(event, true, command, null);
    }

    public static SmartEditResult failed(EditorInputEvent event, EditorCommand command, Throwable failure) {
        return new SmartEditResult(event, true, command, failure);
    }

    public EditorInputEvent event() {
        return event;
    }

    public boolean handled() {
        return handled;
    }

    public boolean isHandled() {
        return handled;
    }

    public boolean failed() {
        return failure != null;
    }

    public Optional<EditorCommand> command() {
        return Optional.ofNullable(command);
    }

    public Optional<Throwable> failure() {
        return Optional.ofNullable(failure);
    }
}
