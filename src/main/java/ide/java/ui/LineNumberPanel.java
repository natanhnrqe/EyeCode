package ide.java.ui;

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.*;

public class LineNumberPanel extends JPanel {
    private final JTextPane textPane;

    public LineNumberPanel(JTextPane textPane) {
        this.textPane = textPane;

        setFont(textPane.getFont());
        setBackground(new Color(43,43,43));
        setForeground(new Color(128,128,128));
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        FontMetrics fm = g.getFontMetrics();
        int lineHeight = fm.getHeight();

        int start = textPane.viewToModel2D(new Point(0,0));
        int end = textPane.viewToModel2D(new Point(0, getHeight()));

        Element root = textPane.getDocument().getDefaultRootElement();

        int startLine = root.getElementIndex(start);
        int endLine = root.getElementIndex(end);

        for (int i = startLine; i <= endLine; i++){

            Element line = root.getElement(i);

            try {
                Rectangle r = textPane.modelToView2D(line.getStartOffset()).getBounds();

                String lineNumbers = String.valueOf(i + 1);

                int stringWidth = fm.stringWidth(lineNumbers);
                int x = getWidth() - stringWidth - 5;
                int y = r.y + fm.getAscent();

                g.drawString(lineNumbers, x, y);
            }catch (Exception e){
                //ignore
            }
        }
    }

    @Override
    public Dimension getPreferredSize() {
        int lines = textPane.getDocument().getDefaultRootElement().getElementCount();
        int digits = String.valueOf(lines).length();

        int width = 10 + digits * getFontMetrics(getFont()).charWidth('0');

        return new Dimension(width, Integer.MAX_VALUE);
    }
}
