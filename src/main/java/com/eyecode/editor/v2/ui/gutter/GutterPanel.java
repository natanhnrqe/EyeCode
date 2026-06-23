package com.eyecode.editor.v2.ui.gutter;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;

public final class GutterPanel extends JPanel {

    private final LineNumberGutter lineNumberGutter;

    public GutterPanel(JTextPane textPane) {
        super(new BorderLayout());
        this.lineNumberGutter = new LineNumberGutter(textPane);
        add(lineNumberGutter, BorderLayout.CENTER);
    }

    public void refresh() {
        revalidate();
        repaint();
    }
}
