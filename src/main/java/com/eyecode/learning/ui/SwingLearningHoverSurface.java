package com.eyecode.learning.ui;

import javax.swing.JTextPane;
import java.awt.IllegalComponentStateException;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.function.IntConsumer;

public final class SwingLearningHoverSurface implements LearningHoverSurface {

    private final JTextPane textPane;
    private final MouseMotionAdapter motionListener;
    private final MouseWheelListener wheelListener;
    private final MouseAdapter mouseListener;
    private final KeyAdapter keyListener;
    private final FocusAdapter focusListener;

    private IntConsumer moveListener;
    private Runnable cancelListener;

    public SwingLearningHoverSurface(JTextPane textPane) {
        this.textPane = textPane;

        motionListener = new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent event) {
                notifyMove(event);
            }
        };

        wheelListener = event -> notifyMove(event);

        mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                cancelHover();
            }
        };

        keyListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                cancelHover();
            }
        };

        focusListener = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent event) {
                cancelHover();
            }
        };

        textPane.addMouseMotionListener(motionListener);
        textPane.addMouseWheelListener(wheelListener);
        textPane.addMouseListener(mouseListener);
        textPane.addKeyListener(keyListener);
        textPane.addFocusListener(focusListener);
    }

    @Override
    public void addMoveListener(IntConsumer listener) {
        moveListener = listener;
    }

    @Override
    public void removeMoveListener(IntConsumer listener) {
        if (moveListener == listener) {
            moveListener = null;
        }
    }

    @Override
    public void addCancelListener(Runnable listener) {
        cancelListener = listener;
    }

    @Override
    public void removeCancelListener(Runnable listener) {
        if (cancelListener == listener) {
            cancelListener = null;
        }
    }

    @Override
    public boolean containsScreen(Point screenPoint) {
        if (screenPoint == null) return false;

        try {
            Point location = textPane.getLocationOnScreen();
            return new Rectangle(location.x, location.y, textPane.getWidth(), textPane.getHeight()).contains(screenPoint);
        } catch (IllegalComponentStateException ex) {
            return false;
        }
    }

    @Override
    public Point pointerScreenLocation() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        return pointerInfo != null ? pointerInfo.getLocation() : null;
    }

    @Override
    public void dispose() {
        textPane.removeMouseMotionListener(motionListener);
        textPane.removeMouseWheelListener(wheelListener);
        textPane.removeMouseListener(mouseListener);
        textPane.removeKeyListener(keyListener);
        textPane.removeFocusListener(focusListener);
        moveListener = null;
        cancelListener = null;
    }

    private void notifyMove(MouseEvent event) {
        if (moveListener != null) {
            moveListener.accept(textPane.viewToModel2D(event.getPoint()));
        }
    }

    private void cancelHover() {
        if (cancelListener != null) {
            cancelListener.run();
        }
    }
}
