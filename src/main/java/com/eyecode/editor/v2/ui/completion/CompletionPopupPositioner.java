package com.eyecode.editor.v2.ui.completion;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.BadLocationException;
import java.awt.Point;
import java.awt.Rectangle;

public final class CompletionPopupPositioner {

    public Point positionFor(JTextPane editor, int caretPosition) {
        try {
            Rectangle caretBounds = editor.modelToView2D(caretPosition).getBounds();
            Point point = new Point(caretBounds.x, caretBounds.y + caretBounds.height);
            SwingUtilities.convertPointToScreen(point, editor);
            point.x -= 8;
            return point;
        } catch (BadLocationException ex) {
            Point fallback = new Point(0, editor.getFontMetrics(editor.getFont()).getHeight());
            SwingUtilities.convertPointToScreen(fallback, editor);
            return fallback;
        }
    }
}
