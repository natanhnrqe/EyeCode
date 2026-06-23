package com.eyecode.editor.v2.ui.demo;

import com.eyecode.editor.v2.ui.RichEditorView;
import com.eyecode.editor.v2.ui.RichEditorViewFactory;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class EditorV2DemoPanel extends JPanel {

    public EditorV2DemoPanel() {
        super(new BorderLayout());
        RichEditorView editorView = RichEditorViewFactory.createEmptyEditor();
        add(editorView, BorderLayout.CENTER);
    }
}
