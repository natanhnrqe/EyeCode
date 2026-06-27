package com.eyecode.editor.v2.command;

import com.eyecode.editor.v2.EditorDocument;

import java.util.Objects;

public final class DeleteTextCommand implements EditCommand {

    private final int offset;
    private final String removedText;

    public DeleteTextCommand(int offset, String removedText) {
        if (offset < 0) throw new IllegalArgumentException("offset < 0");
        this.offset = offset;
        this.removedText = removedText == null ? "" : removedText;
    }

    @Override
    public void execute(EditorDocument document) {
        document.delete(offset, offset + removedText.length());
    }

    @Override
    public void undo(EditorDocument document) {
        document.insert(offset, removedText);
    }

    public int getOffset() { return offset; }

    public String getRemovedText() { return removedText; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DeleteTextCommand that)) return false;
        return offset == that.offset && Objects.equals(removedText, that.removedText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(offset, removedText);
    }
}
