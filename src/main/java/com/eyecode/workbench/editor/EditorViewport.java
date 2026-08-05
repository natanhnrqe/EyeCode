package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorPosition;

import java.nio.file.Path;

public record EditorViewport(Path file, EditorPosition caret, EditorScroll scroll) {

    public static EditorViewport initial(Path file) {
        return new EditorViewport(file, new EditorPosition(0, 0), EditorScroll.zero());
    }
}
