package com.eyecode.editor.v2.command;

import com.eyecode.editor.v2.EditorDocument;

public interface EditCommand {
    void execute(EditorDocument document);
    void undo(EditorDocument document);
}
