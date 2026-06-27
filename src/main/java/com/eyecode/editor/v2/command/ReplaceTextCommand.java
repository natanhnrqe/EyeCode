package com.eyecode.editor.v2.command;

import com.eyecode.editor.v2.EditorDocument;

import java.util.Objects;

public final class ReplaceTextCommand implements EditCommand {

    private final String oldText;
    private final String newText;

    public ReplaceTextCommand(String oldText, String newText) {
        this.oldText = oldText == null ? "" : oldText;
        this.newText = newText == null ? "" : newText;
    }

    @Override
    public void execute(EditorDocument document) {
        document.setText(newText);
    }

    @Override
    public void undo(EditorDocument document) {
        document.setText(oldText);
    }

    public String getOldText() { return oldText; }

    public String getNewText() { return newText; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ReplaceTextCommand that)) return false;
        return Objects.equals(oldText, that.oldText) && Objects.equals(newText, that.newText);
    }

    @Override
    public int hashCode() {
        return Objects.hash(oldText, newText);
    }
}
