package com.eyecode.javafx.learning;

import java.awt.Point;
import java.util.function.Supplier;
import com.eyecode.learning.ui.LearningHoverSurface;

final class JavaFxLearningAnchor {

    private Supplier<Point> pointSupplier = () -> null;
    private Supplier<javafx.stage.Window> windowSupplier = () -> null;

    void follow(JavaFxLearningHoverSurface surface) {
        follow(surface, surface::ownerWindow);
    }

    void follow(LearningHoverSurface surface, Supplier<javafx.stage.Window> windowSupplier) {
        pointSupplier = surface::pointerScreenLocation;
        this.windowSupplier = windowSupplier;
    }

    Point point() {
        return pointSupplier.get();
    }

    javafx.stage.Window window() {
        return windowSupplier.get();
    }
}
