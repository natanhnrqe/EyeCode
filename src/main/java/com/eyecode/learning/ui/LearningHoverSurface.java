package com.eyecode.learning.ui;

import java.awt.Point;
import java.util.function.IntConsumer;

public interface LearningHoverSurface {

    void addMoveListener(IntConsumer listener);

    void removeMoveListener(IntConsumer listener);

    void addCancelListener(Runnable listener);

    void removeCancelListener(Runnable listener);

    boolean containsScreen(Point screenPoint);

    Point pointerScreenLocation();

    void dispose();
}
