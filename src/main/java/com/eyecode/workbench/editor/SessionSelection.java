package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;

final class SessionSelection {

    EditorPosition caret;
    EditorSelection selection;
    EditorScroll scroll;

    SessionSelection() {
        this.caret = new EditorPosition(0, 0);
        this.selection = new EditorSelection(this.caret, this.caret);
        this.scroll = EditorScroll.zero();
    }
}
