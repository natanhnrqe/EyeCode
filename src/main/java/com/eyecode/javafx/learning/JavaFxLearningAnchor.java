package com.eyecode.javafx.learning;

import java.awt.Point;
import java.util.function.Supplier;

final class JavaFxLearningAnchor {

    private Supplier<Point> pointSupplier = () -> null;
    private Supplier<javafx.stage.Window> windowSupplier = () -> null;

    void follow(JavaFxLearningHoverSurface surface) {
        pointSupplier = surface::pointerScreenLocation;
        windowSupplier = surface::ownerWindow;
    }

    Point point() {
        return pointSupplier.get();
    }

    javafx.stage.Window window() {
        return windowSupplier.get();
    }
}
