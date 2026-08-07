package com.eyecode.editor.intelligence.pipeline;

/**
 * A named, undoable editor action expressed against the document engine.
 * <p>
 * Commands are pure Core: they never reference Swing or JavaFX. UI layers
 * translate native gestures into {@link EditorInputEvent}s and the pipeline
 * resolves them into {@code EditorCommand}s.
 */
public interface EditorCommand {

    String name();

    void execute(EditorCommandContext context);

    void undo(EditorCommandContext context);

    default boolean canExecute(EditorCommandContext context) {
        return true;
    }
}
