package com.eyecode.editor.v2.ui;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;

public final class RichEditorViewFactory {

    private RichEditorViewFactory() {
    }

    public static RichEditorView createEmptyEditor() {
        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);
        return new RichEditorView(buffer);
    }
}
