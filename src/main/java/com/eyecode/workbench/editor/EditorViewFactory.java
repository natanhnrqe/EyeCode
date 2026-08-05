package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorBuffer;

import java.nio.file.Path;

public interface EditorViewFactory {

    EditorView create(EditorBuffer buffer);

    boolean supports(Path file);

    String id();
}
