package com.eyecode.editor.v2.command;

import com.eyecode.editor.v2.EditorDocument;

import java.util.Objects;

public final class InsertTextCommand implements EditCommand {

    private final int offset;
    private final String text;

    public InsertTextCommand(int offset, String text) {
        if (offset < 0) throw new IllegalArgumentException("offset < 0");
        this.offset = offset;
        this.text = text == null ? "" : text;
    }

    @Override
    public void execute(EditorDocument document) {
        document.insert(offset, text);
    }

    @Override
    public void undo(EditorDocument document) {
        document.delete(offset, offset + text.length());
    }

    public int getOffset() { return offset; }

    public String getText() { return text; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof InsertTextCommand that)) return false;
        return offset == that.offset && Objects.equals(text, that.text);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, text);
    }
}
