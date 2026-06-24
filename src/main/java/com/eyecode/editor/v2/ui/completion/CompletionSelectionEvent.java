package com.eyecode.editor.v2.ui.completion;

import com.eyecode.editor.v2.completion.CompletionItem;

public final class CompletionSelectionEvent {

    private final CompletionItem selectedItem;
    private final int index;
    private final int caretPosition;

    public CompletionSelectionEvent(CompletionItem selectedItem, int index, int caretPosition) {
        this.selectedItem = selectedItem;
        this.index = index;
        this.caretPosition = caretPosition;
    }

    public CompletionItem getSelectedItem() { return selectedItem; }

    public int getIndex() { return index; }

    public int getCaretPosition() { return caretPosition; }
}
