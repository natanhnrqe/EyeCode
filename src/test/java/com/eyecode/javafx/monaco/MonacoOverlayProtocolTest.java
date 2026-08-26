package com.eyecode.javafx.monaco;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MonacoOverlayProtocolTest {

    @Test
    void pointerEventsAreTypedAndCarryGeneration() {
        MonacoOverlayEvent event = JavaFxMonacoEditorSurface.parseOverlayEventForTest(
                "{\"kind\":\"overlayPointer\",\"overlayId\":\"learning\","
                        + "\"entered\":true,\"generation\":12}");

        assertEquals(MonacoOverlayEvent.Type.POINTER_ENTER, event.type());
        assertEquals("learning", event.overlayId());
        assertEquals(12L, event.generation());
    }

    @Test
    void actionsRejectUnknownValues() {
        assertNull(JavaFxMonacoEditorSurface.parseOverlayEventForTest(
                "{\"kind\":\"overlayAction\",\"overlayId\":\"learning\","
                        + "\"action\":\"UNKNOWN\",\"generation\":1}"));
    }

    @Test
    void actionsCarryLearningTarget() {
        MonacoOverlayEvent event = JavaFxMonacoEditorSurface.parseOverlayEventForTest(
                "{\"kind\":\"overlayAction\",\"overlayId\":\"learning\","
                        + "\"action\":\"NAVIGATE_LEARNING\",\"target\":\"java/types/class\","
                        + "\"generation\":4}");

        assertEquals(MonacoOverlayAction.NAVIGATE_LEARNING, event.action());
        assertEquals("java/types/class", event.target());
    }

    @Test
    void domGraceCompletionIsTypedAsOverlayHidden() {
        MonacoOverlayEvent event = JavaFxMonacoEditorSurface.parseOverlayEventForTest(
                "{\"kind\":\"overlayHidden\",\"overlayId\":\"learning\",\"generation\":9}");

        assertEquals(MonacoOverlayEvent.Type.HIDDEN, event.type());
        assertEquals("learning", event.overlayId());
        assertEquals(9L, event.generation());
    }
}
