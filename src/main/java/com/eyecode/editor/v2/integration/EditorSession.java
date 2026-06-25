package com.eyecode.editor.v2.integration;

import com.eyecode.editor.EditorTab;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.ui.RichEditorView;

public final class EditorSession {

    private final EditorTab tab;
    private final EditorBuffer buffer;
    private final RichEditorView view;

    public EditorSession(EditorTab tab, EditorBuffer buffer, RichEditorView view) {
        this.tab = tab;
        this.buffer = buffer;
        this.view = view;
    }

    public EditorTab getTab() { return tab; }

    public EditorBuffer getBuffer() { return buffer; }

    public RichEditorView getView() { return view; }
}
