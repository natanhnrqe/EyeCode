package com.eyecode.learning.ui;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

class HoverStateMachineTest {

    private final AtomicLong clock = new AtomicLong();
    private final LongSupplier supplier = clock::get;

    @Test
    void waitsBeforeShowing() {
        HoverStateMachine machine = new HoverStateMachine(supplier);

        machine.enter("class:1:6");

        assertEquals(HoverState.WAITING, machine.getState());
        assertFalse(machine.canShow());

        clock.addAndGet(499L);
        assertFalse(machine.canShow());

        clock.addAndGet(1L);
        assertTrue(machine.canShow());
        assertEquals(HoverState.VISIBLE, machine.getState());
    }

    @Test
    void hoverUpdateKeepsPopupVisible() {
        HoverStateMachine machine = new HoverStateMachine(supplier);

        machine.enter("class:1:6");
        clock.addAndGet(500L);
        assertTrue(machine.canShow());

        machine.enter("interface:1:10");

        assertEquals(HoverState.VISIBLE, machine.getState());
        assertEquals("interface:1:10", machine.getActiveKey());
        assertFalse(machine.canHide());
    }

    @Test
    void interactsWithPopupAndHidesAfterDelay() {
        HoverStateMachine machine = new HoverStateMachine(supplier);

        machine.enter("class:1:6");
        clock.addAndGet(500L);
        assertTrue(machine.canShow());

        machine.enter("class:1:6");
        machine.leave();
        assertFalse(machine.canHide());

        machine.setPopupHover(true);
        assertEquals(HoverState.INTERACTING, machine.getState());

        machine.setPopupHover(false);
        machine.leave();
        assertFalse(machine.canHide());

        clock.addAndGet(299L);
        assertFalse(machine.canHide());

        clock.addAndGet(1L);
        assertTrue(machine.canHide());
        assertEquals(HoverState.IDLE, machine.getState());
    }

    @Test
    void leavingTheHoveredSymbolStartsTheHideCountdown() {
        HoverStateMachine machine = new HoverStateMachine(supplier);

        machine.enter("class:1:6");
        clock.addAndGet(500L);
        assertTrue(machine.canShow());

        machine.leave();
        assertEquals(HoverState.HIDING, machine.getState());

        clock.addAndGet(300L);
        assertTrue(machine.canHide());
        assertEquals(HoverState.IDLE, machine.getState());
    }

    @Test
    void resetClearsStateImmediately() {
        HoverStateMachine machine = new HoverStateMachine(supplier);

        machine.enter("class:1:6");
        machine.reset();

        assertEquals(HoverState.IDLE, machine.getState());
        assertNull(machine.getActiveKey());
        assertFalse(machine.canShow());
        assertFalse(machine.canHide());
    }
}
