package com.eyecode.workbench.editor;

public record EditorScroll(double xScrollValue, double yScrollValue) {

    public static EditorScroll zero() {
        return new EditorScroll(0.0, 0.0);
    }
}
