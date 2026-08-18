package com.eyecode.workbench.editor;

public interface EditorView {

    Object getNativeView();

    void refreshFromDocument();

    default void bindNavigation(EditorManager manager, String sessionId) {
    }

    void dispose();
}
