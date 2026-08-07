package com.eyecode.editor.intelligence.pipeline;

/**
 * Raw, UI-free representation of an editor input.
 * <p>
 * Swing and JavaFX views translate native key events into these value objects
 * before handing them to the typing pipeline.
 */
public record EditorInputEvent(InputKind kind, String text, int offset, boolean shiftPressed) {

    public enum InputKind {
        TEXT,
        NEW_LINE,
        BACKSPACE,
        DELETE,
        TAB,
        ENTER,
        PASTE,
        UNDO,
        REDO
    }

    public static EditorInputEvent text(int offset, String text) {
        return new EditorInputEvent(InputKind.TEXT, text == null ? "" : text, offset, false);
    }

    public boolean isText() {
        return kind == InputKind.TEXT || kind == InputKind.PASTE || kind == InputKind.NEW_LINE;
    }
}
