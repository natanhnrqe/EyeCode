package com.eyecode.javafx.learning;

import com.eyecode.learning.ui.LearningHoverSurface;
import javafx.animation.PauseTransition;
import javafx.scene.layout.Region;
import javafx.util.Duration;

import java.awt.Point;
import java.util.function.IntConsumer;
import java.util.function.Supplier;

public final class MonacoLearningHoverSurface implements LearningHoverSurface {
    private final Region host;
    private final PauseTransition throttle = new PauseTransition(Duration.millis(40));
    private IntConsumer moveListener;
    private Runnable cancelListener;
    private Runnable leaveListener;
    private Point pointer;
    private int pendingOffset;

    public MonacoLearningHoverSurface(Region host, Supplier<javafx.stage.Window> windowSupplier) {
        this.host = host;
        throttle.setOnFinished(event -> {
            if (moveListener != null) moveListener.accept(pendingOffset);
        });
    }

    public void move(int offset, double x, double y) {
        pendingOffset = offset;
        if (throttle.getStatus() != javafx.animation.Animation.Status.RUNNING) {
            throttle.playFromStart();
        }
    }

    public void leave() {
        throttle.stop();
        if (leaveListener != null) leaveListener.run();
    }

    @Override public void addMoveListener(IntConsumer listener) { moveListener = listener; }
    @Override public void removeMoveListener(IntConsumer listener) { if (moveListener == listener) moveListener = null; }
    @Override public void addCancelListener(Runnable listener) { cancelListener = listener; }
    @Override public void removeCancelListener(Runnable listener) { if (cancelListener == listener) cancelListener = null; }
    @Override public void addLeaveListener(Runnable listener) { leaveListener = listener; }
    @Override public void removeLeaveListener(Runnable listener) { if (leaveListener == listener) leaveListener = null; }

    @Override public boolean containsScreen(Point screenPoint) {
        return false;
    }

    @Override public Point pointerScreenLocation() { return pointer == null ? null : new Point(pointer); }
    @Override public void dispose() {
        throttle.stop();
        moveListener = null;
        cancelListener = null;
        leaveListener = null;
        pointer = null;
    }
}
