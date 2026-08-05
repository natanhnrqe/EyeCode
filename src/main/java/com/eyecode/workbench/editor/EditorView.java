package com.eyecode.workbench.editor;

public interface EditorView {

    Object getNativeView();

    void refreshFromDocument();

    void dispose();
}
