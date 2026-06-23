package com.eyecode.editor.v2.ui.demo;

import com.eyecode.editor.v2.ui.EditorView;
import com.eyecode.editor.v2.ui.EditorViewFactory;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class EditorV2DemoPanel extends JPanel {

    public EditorV2DemoPanel() {
        super(new BorderLayout());
        EditorView editorView = EditorViewFactory.createEmptyEditor();
        add(editorView, BorderLayout.CENTER);
    }
}
