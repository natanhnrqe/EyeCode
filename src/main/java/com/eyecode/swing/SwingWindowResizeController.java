package com.eyecode.swing;

import javax.swing.JFrame;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

final class SwingWindowResizeController extends MouseAdapter {
    private static final int BORDER = 7;
    private final JFrame frame;
    private int direction;
    private Point press;
    private Rectangle bounds;

    SwingWindowResizeController(JFrame frame, Component target) {
        this.frame = frame;
        target.addMouseListener(this);
        target.addMouseMotionListener(this);
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        direction = direction(event.getX(), event.getY());
        event.getComponent().setCursor(Cursor.getPredefinedCursor(cursor(direction)));
    }

    @Override
    public void mousePressed(MouseEvent event) {
        direction = direction(event.getX(), event.getY());
        if (direction == 0 || frame.getExtendedState() == JFrame.MAXIMIZED_BOTH) return;
        press = event.getLocationOnScreen();
        bounds = frame.getBounds();
    }

    @Override
    public void mouseDragged(MouseEvent event) {
        if (press == null || bounds == null) return;
        Point current = event.getLocationOnScreen();
        int dx = current.x - press.x;
        int dy = current.y - press.y;
        Rectangle next = new Rectangle(bounds);
        if ((direction & 1) != 0) { next.x += dx; next.width -= dx; }
        if ((direction & 2) != 0) next.width += dx;
        if ((direction & 4) != 0) { next.y += dy; next.height -= dy; }
        if ((direction & 8) != 0) next.height += dy;
        if (next.width >= frame.getMinimumSize().width && next.height >= frame.getMinimumSize().height) frame.setBounds(next);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        press = null;
        bounds = null;
    }

    private int direction(int x, int y) {
        int value = 0;
        if (x <= BORDER) value |= 1;
        if (x >= frame.getWidth() - BORDER) value |= 2;
        if (y <= BORDER) value |= 4;
        if (y >= frame.getHeight() - BORDER) value |= 8;
        return value;
    }

    private static int cursor(int direction) {
        return switch (direction) {
            case 1 -> Cursor.W_RESIZE_CURSOR;
            case 2 -> Cursor.E_RESIZE_CURSOR;
            case 4 -> Cursor.N_RESIZE_CURSOR;
            case 8 -> Cursor.S_RESIZE_CURSOR;
            case 5 -> Cursor.NW_RESIZE_CURSOR;
            case 6 -> Cursor.NE_RESIZE_CURSOR;
            case 9 -> Cursor.SW_RESIZE_CURSOR;
            case 10 -> Cursor.SE_RESIZE_CURSOR;
            default -> Cursor.DEFAULT_CURSOR;
        };
    }
}
