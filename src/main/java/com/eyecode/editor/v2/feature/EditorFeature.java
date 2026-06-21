package com.eyecode.editor.v2.feature;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorPosition;

public interface EditorFeature {

    void attach(EditorBuffer buffer);

    void detach();

    void onDocumentChanged();

    void onCaretMoved(EditorPosition position);
}
