package com.eyecode.editor.v2.ui.gutter;

import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.JPanel;
import javax.swing.JTextPane;
import java.awt.BorderLayout;

public final class GutterPanel extends JPanel {

    private final LineNumberGutter lineNumberGutter;

    public GutterPanel(JTextPane textPane) {
        super(new BorderLayout());
        setBackground(ColorManager.LINE_NUMBER_BG);
        setBorder(javax.swing.BorderFactory.createMatteBorder(0, 0, 0, 1, ColorManager.BORDER_DIVIDER));
        this.lineNumberGutter = new LineNumberGutter(textPane);
        add(lineNumberGutter, BorderLayout.CENTER);
    }

    public void refresh() {
        revalidate();
        repaint();
    }
}
