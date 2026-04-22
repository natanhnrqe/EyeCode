package ide.java.ui;

import javax.swing.*;
import javax.swing.text.Element;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

public class LineNumberPanel extends JPanel {

    private final JTextPane textPane;

    private final Set<Integer> breakpoints = new HashSet<>();

    public LineNumberPanel(JTextPane textPane) {
        this.textPane = textPane;

        setFont(textPane.getFont());
        setBackground(new Color(43,43,43));
        setForeground(new Color(128,128,128));
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int y = e.getY();

                int offset = textPane.viewToModel2D(new Point(0, y));

                Element root = textPane.getDocument().getDefaultRootElement();
                int line = root.getElementIndex(offset);

                toggleBreakpoint(line);
                repaint();
            }
        });
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

                if (breakpoints.contains(i)){
                    g.setColor(Color.RED);

                    int size = 8;
                    int xCircle = 4;
                    int yCircle = r.y + (r.height - size) / 2;

                    g.fillOval(xCircle, yCircle, size, size);

                    g.setColor(getForeground());
                }
            }catch (Exception e){
                //ignore
            }


        }
    }

    @Override
    public Dimension getPreferredSize() {
        int lines = textPane.getDocument().getDefaultRootElement().getElementCount();
        int digits = String.valueOf(lines).length();

        int width = 20 + digits * getFontMetrics(getFont()).charWidth('0');

        return new Dimension(width, Integer.MAX_VALUE);
    }

    private void toggleBreakpoint(int line){
        if (breakpoints.contains(line)){
            breakpoints.remove(line);
        }else {
            breakpoints.add(line);
        }

    }
}
