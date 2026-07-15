package com.eyecode.learning.ui;

import org.junit.jupiter.api.Test;

import javax.swing.JTextPane;
import java.awt.event.MouseWheelEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SwingLearningHoverSurfaceTest {

    @Test
    void mouseWheelRefreshesHoverOffset() {
        JTextPane textPane = new JTextPane();
        textPane.setText("class Sample {}");
        textPane.setSize(400, 200);
        SwingLearningHoverSurface surface = new SwingLearningHoverSurface(textPane);
        AtomicInteger lastOffset = new AtomicInteger(-1);

        surface.addMoveListener(lastOffset::set);

        MouseWheelEvent wheelEvent = new MouseWheelEvent(
                textPane,
                MouseWheelEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                0,
                20,
                10,
                0,
                false,
                MouseWheelEvent.WHEEL_UNIT_SCROLL,
                1,
                1
        );

        for (var listener : textPane.getMouseWheelListeners()) {
            listener.mouseWheelMoved(wheelEvent);
        }

        assertTrue(lastOffset.get() >= 0);
        surface.dispose();
    }
}
