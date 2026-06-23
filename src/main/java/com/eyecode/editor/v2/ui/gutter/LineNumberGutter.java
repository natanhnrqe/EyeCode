package com.eyecode.editor.v2.ui.gutter;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;

public final class LineNumberGutter extends JComponent {

    private static final int HORIZONTAL_PADDING = 8;

    private final JTextPane textPane;

    public LineNumberGutter(JTextPane textPane) {
        this.textPane = textPane;
        setFont(textPane.getFont());
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Font originalFont = g.getFont();
        Font normalFont = textPane.getFont();
        Font boldFont = normalFont.deriveFont(Font.BOLD);
        FontMetrics metrics = g.getFontMetrics(normalFont);
        int currentLine = getCurrentCaretLine();

        Element root = textPane.getDocument().getDefaultRootElement();
        for (int i = 0; i < root.getElementCount(); i++) {
            int lineStartOffset = root.getElement(i).getStartOffset();
            try {
                Rectangle rect = textPane.modelToView(lineStartOffset);
                if (rect == null) continue;

                boolean active = i == currentLine;
                g.setFont(active ? boldFont : normalFont);
                String text = String.valueOf(i + 1);
                int x = getPreferredSize().width - HORIZONTAL_PADDING - metrics.stringWidth(text);
                int y = rect.y + rect.height - metrics.getDescent();
                g.drawString(text, x, y);
            } catch (BadLocationException ignored) {
                // Invalid offsets are skipped; the document may be changing during repaint.
            }
        }

        g.setFont(originalFont);
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(calculatePreferredWidth(), textPane.getPreferredSize().height);
    }

    private int calculatePreferredWidth() {
        Element root = textPane.getDocument().getDefaultRootElement();
        int lineCount = Math.max(1, root.getElementCount());
        int digits = String.valueOf(lineCount).length();
        FontMetrics metrics = getFontMetrics(textPane.getFont());
        return (HORIZONTAL_PADDING * 2) + metrics.charWidth('0') * digits;
    }

    private int getCurrentCaretLine() {
        Element root = textPane.getDocument().getDefaultRootElement();
        return root.getElementIndex(textPane.getCaretPosition());
    }
}
