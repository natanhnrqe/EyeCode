package com.eyecode.editor.v2.ui.gutter;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.JComponent;
import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Element;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;

public final class LineNumberGutter extends JComponent {

    private static final int RIGHT_PADDING = 10;
    private static final int LEFT_PADDING = 8;
    private static final int MIN_WIDTH = 36;

    private final JTextPane textPane;
    private final Font gutterFont;

    public LineNumberGutter(JTextPane textPane) {
        this.textPane = textPane;
        this.gutterFont = TypographyManager.UI_CODE();
        setFont(gutterFont);
        setBackground(ColorManager.LINE_NUMBER_BG);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g.create();
        try {
            g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);

            g2d.setColor(ColorManager.LINE_NUMBER_BG);
            g2d.fillRect(0, 0, getWidth(), getHeight());

            Font normalFont = gutterFont;
            FontMetrics metrics = g2d.getFontMetrics(normalFont);
            int currentLine = getCurrentCaretLine();

            Element root = textPane.getDocument().getDefaultRootElement();
            int totalWidth = getPreferredSize().width;

            for (int i = 0; i < root.getElementCount(); i++) {
                int lineStartOffset = root.getElement(i).getStartOffset();
                try {
                    Rectangle rect = textPane.modelToView(lineStartOffset);
                    if (rect == null) continue;

                    boolean active = i == currentLine;
                    g2d.setFont(normalFont);
                    g2d.setColor(active ? ColorManager.TEXT_PRIMARY : ColorManager.LINE_NUMBER_FG);
                    FontMetrics fm = g2d.getFontMetrics(normalFont);
                    String text = String.valueOf(i + 1);
                    int textWidth = fm.stringWidth(text);
                    int x = totalWidth - RIGHT_PADDING - textWidth;
                    int y = rect.y + (rect.height + fm.getAscent() - fm.getDescent()) / 2;
                    g2d.drawString(text, x, y);
                } catch (BadLocationException ignored) {
                }
            }
        } finally {
            g2d.dispose();
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return new Dimension(calculatePreferredWidth(), textPane.getPreferredSize().height);
    }

    private int calculatePreferredWidth() {
        Element root = textPane.getDocument().getDefaultRootElement();
        int lineCount = Math.max(1, root.getElementCount());
        int digits = String.valueOf(lineCount).length();
        FontMetrics metrics = getFontMetrics(gutterFont);
        int width = LEFT_PADDING + metrics.charWidth('0') * digits + RIGHT_PADDING;
        return Math.max(width, MIN_WIDTH);
    }

    private int getCurrentCaretLine() {
        Element root = textPane.getDocument().getDefaultRootElement();
        return root.getElementIndex(textPane.getCaretPosition());
    }
}
