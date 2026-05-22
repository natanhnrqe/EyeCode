package com.eyecode.ui;

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

    private int currentLine = -1;

    private BreakpointListener listener;


    public LineNumberPanel(JTextPane textPane) {
        this.textPane = textPane;

        setFont(textPane.getFont());
        setBackground(new Color(39,41,42));
        setForeground(new Color(128,128,128));
        setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
        setBorder(null);

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int y = e.getY();

                int offset = textPane.viewToModel2D(new Point(0, y));

                Element root = textPane.getDocument().getDefaultRootElement();
                int line = root.getElementIndex(offset);

                toggleBreakpoint(line);
                repaint();

                if (listener != null){
                    listener.onBreakpointChanged();
                }

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
                // 1. FUNDO BASE (sempre)
                g.setColor(new Color(30, 30, 30));
                g.fillRect(0, r.y, getWidth(), r.height);

                // fundo breakpoint
                if (breakpoints.contains(i)){
                    g.setColor(new Color(30, 30, 30));
                    g.fillRect(0, r.y, getWidth(), r.height);
                }

                // linha atual
                if (i == currentLine){
                    g.setColor(new Color(39,41,42));
                    g.fillRect(0, r.y, getWidth(), r.height);
                }

                String lineNumbers = String.valueOf(i + 1);

                int stringWidth = fm.stringWidth(lineNumbers);
                int padding = 8;
                int x = getWidth() - stringWidth - padding;
                int y = r.y + fm.getAscent();

                // reset cor
                if (i == currentLine){
                    g.setColor(Color.WHITE);
                }else {
                    g.setColor(getForeground());
                }

                if (breakpoints.contains(i)){
                    g.setColor(new Color(255, 85, 85));

                    int size = 8;
                    int xCircle = 4;
                    int yCircle = r.y + (r.height - size) / 2;

                    g.fillOval(xCircle, yCircle, size, size);
                }else {
                    if (i == currentLine){
                        g.setColor(Color.WHITE);
                    }else {
                        g.setColor(getForeground());
                    }
                    g.drawString(lineNumbers, x, y);
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

        int width = 10 + digits * getFontMetrics(getFont()).charWidth('0');

        return new Dimension(width, Integer.MAX_VALUE);
    }

    private void toggleBreakpoint(int line){
        if (breakpoints.contains(line)){
            breakpoints.remove(line);
        }else {
            breakpoints.add(line);
        }
    }

    public Set<Integer> getBreakpoints() {
        return breakpoints;
    }

    public void setCurrentLine(int currentLine) {
        this.currentLine = currentLine;
        repaint();
    }

    public void setListener(BreakpointListener listener) {
        this.listener = listener;
    }
}


