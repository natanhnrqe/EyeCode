package com.eyecode.editor.v2;

public final class EditorBuffer {

    private final EditorDocument document;
    private EditorPosition caret;
    private EditorSelection selection;

    public EditorBuffer(EditorDocument document) {
        this.document = document;
        this.caret = new EditorPosition(0, 0);
        this.selection = new EditorSelection(caret, caret);
    }

    public void moveCaret(EditorPosition position) {
        this.caret = position;
        this.selection = new EditorSelection(position, position);
    }

    public void setSelection(EditorSelection selection) {
        this.selection = selection;
    }

    public EditorDocument getDocument() {
        return document;
    }

    public EditorPosition getCaret() { return caret; }

    public EditorSelection getSelection() { return selection; }
}
