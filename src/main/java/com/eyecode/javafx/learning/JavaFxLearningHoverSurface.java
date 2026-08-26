package com.eyecode.javafx.learning;

import com.eyecode.learning.ui.LearningHoverSurface;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.Window;
import org.fxmisc.richtext.CodeArea;

import java.awt.Point;
import java.util.function.IntConsumer;

public final class JavaFxLearningHoverSurface implements LearningHoverSurface {

    private final CodeArea codeArea;
    private final javafx.event.EventHandler<MouseEvent> mouseHandler = this::handleMouseMove;
    private final javafx.event.EventHandler<MouseEvent> mouseExitHandler = event -> leaveHover();
    private final javafx.event.EventHandler<KeyEvent> keyHandler = event -> cancelHover();
    private IntConsumer moveListener;
    private Runnable cancelListener;
    private Runnable leaveListener;
    private Runnable pointerObserver;
    private Point lastPointer;

    public JavaFxLearningHoverSurface(CodeArea codeArea) {
        this.codeArea = codeArea;
        codeArea.addEventHandler(MouseEvent.MOUSE_MOVED, mouseHandler);
        codeArea.addEventHandler(MouseEvent.MOUSE_DRAGGED, mouseHandler);
        codeArea.addEventHandler(MouseEvent.MOUSE_EXITED, mouseExitHandler);
        codeArea.addEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
        codeArea.focusedProperty().addListener((observable, oldValue, focused) -> {
            if (!focused) {
                cancelHover();
            }
        });
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
    public void addLeaveListener(Runnable listener) {
        leaveListener = listener;
    }

    @Override
    public void removeLeaveListener(Runnable listener) {
        if (leaveListener == listener) {
            leaveListener = null;
        }
    }

    @Override
    public boolean containsScreen(Point screenPoint) {
        if (screenPoint == null) {
            return false;
        }
        Bounds bounds = codeArea.localToScreen(codeArea.getBoundsInLocal());
        return bounds != null && bounds.contains(screenPoint.x, screenPoint.y);
    }

    @Override
    public Point pointerScreenLocation() {
        try {
            var pointerInfo = java.awt.MouseInfo.getPointerInfo();
            if (pointerInfo != null) {
                return pointerInfo.getLocation();
            }
        } catch (RuntimeException ignored) {
        }
        return lastPointer == null ? null : new Point(lastPointer);
    }

    Window ownerWindow() {
        return codeArea.getScene() == null ? null : codeArea.getScene().getWindow();
    }

    void setPointerObserver(Runnable observer) {
        pointerObserver = observer;
    }

    @Override
    public void dispose() {
        codeArea.removeEventHandler(MouseEvent.MOUSE_MOVED, mouseHandler);
        codeArea.removeEventHandler(MouseEvent.MOUSE_DRAGGED, mouseHandler);
        codeArea.removeEventHandler(MouseEvent.MOUSE_EXITED, mouseExitHandler);
        codeArea.removeEventHandler(KeyEvent.KEY_PRESSED, keyHandler);
        moveListener = null;
        cancelListener = null;
        leaveListener = null;
        pointerObserver = null;
        lastPointer = null;
    }

    private void handleMouseMove(MouseEvent event) {
        Point2D screen = codeArea.localToScreen(event.getX(), event.getY());
        if (screen != null) {
            lastPointer = new Point((int) Math.round(screen.getX()), (int) Math.round(screen.getY()));
        }
        if (pointerObserver != null) {
            pointerObserver.run();
        }
        if (moveListener == null) {
            return;
        }
        moveListener.accept(codeArea.hit(event.getX(), event.getY()).getInsertionIndex());
    }

    private void cancelHover() {
        if (cancelListener != null) {
            cancelListener.run();
        }
    }

    private void leaveHover() {
        if (leaveListener != null) {
            leaveListener.run();
        }
    }
}
