package com.eyecode.workbench.toolwindow;

import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.ToolWindowActivatedEvent;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ToolWindowManagerTest {

    private ToolWindowManager newManager() {
        ToolWindowManager manager = new ToolWindowManager();
        manager.register("project", "Project", "PROJECT", ToolWindowPosition.LEFT);
        manager.register("search", "Search", "SEARCH", ToolWindowPosition.LEFT);
        manager.register("terminal", "Terminal", "TERMINAL", ToolWindowPosition.BOTTOM);
        manager.register("problems", "Problems", "PROBLEM", ToolWindowPosition.BOTTOM);
        return manager;
    }

    @Test
    void registerPersistsWindowAndFiresChange() {
        ToolWindowManager manager = new ToolWindowManager();
        AtomicInteger changes = new AtomicInteger();
        manager.addChangeListener(changes::incrementAndGet);

        manager.register("explorer", "Explorer", "PROJECT", ToolWindowPosition.LEFT);

        assertTrue(manager.getToolWindow("explorer").isPresent());
        assertEquals(1, changes.get());

        ToolWindow window = manager.getToolWindow("explorer").orElseThrow();
        assertEquals("explorer", window.getId());
        assertEquals("Explorer", window.getTitle());
        assertEquals("PROJECT", window.getIconKey());
        assertEquals(ToolWindowPosition.LEFT, window.getPosition());
        assertFalse(window.isVisible());
        assertFalse(window.isActive());
    }

    @Test
    void registerRejectsDuplicateId() {
        ToolWindowManager manager = newManager();
        assertThrows(IllegalStateException.class, () ->
                manager.register("project", "Outro", "SEARCH", ToolWindowPosition.LEFT));
    }

    @Test
    void registerValidatesNulls() {
        ToolWindowManager manager = new ToolWindowManager();
        assertThrows(IllegalArgumentException.class, () -> manager.register(null));
        assertThrows(IllegalArgumentException.class, () ->
                manager.register("  ", "Titulo", "ICON", ToolWindowPosition.LEFT));
    }

    @Test
    void activateMakesVisibleAndActiveWithinPosition() {
        ToolWindowManager manager = newManager();

        manager.activate("project");
        assertTrue(manager.getToolWindow("project").orElseThrow().isActive());
        assertSame(manager.getToolWindow("project").orElseThrow(),
                manager.getActive(ToolWindowPosition.LEFT));

        manager.activate("search");
        assertFalse(manager.getToolWindow("project").orElseThrow().isActive());
        assertTrue(manager.getToolWindow("search").orElseThrow().isActive());
        assertSame(manager.getToolWindow("search").orElseThrow(),
                manager.getActive(ToolWindowPosition.LEFT));
    }

    @Test
    void positionsAreIndependent() {
        ToolWindowManager manager = newManager();

        manager.activate("project");
        manager.activate("terminal");

        assertSame(manager.getToolWindow("project").orElseThrow(),
                manager.getActive(ToolWindowPosition.LEFT));
        assertSame(manager.getToolWindow("terminal").orElseThrow(),
                manager.getActive(ToolWindowPosition.BOTTOM));
    }

    @Test
    void openActivatesWindow() {
        ToolWindowManager manager = newManager();
        manager.open("project");
        assertTrue(manager.getToolWindow("project").orElseThrow().isVisible());
        assertTrue(manager.getToolWindow("project").orElseThrow().isActive());
    }

    @Test
    void closeDeactivatesAndActivatesAnotherVisible() {
        ToolWindowManager manager = newManager();
        manager.activate("project");
        manager.activate("search");

        manager.close("search");
        assertFalse(manager.getToolWindow("search").orElseThrow().isActive());
        assertSame(manager.getToolWindow("project").orElseThrow(),
                manager.getActive(ToolWindowPosition.LEFT));
    }

    @Test
    void unregisterActiveWindowActivatesNext() {
        ToolWindowManager manager = newManager();
        manager.activate("project");
        manager.activate("search");

        manager.unregister("search");
        assertNull(manager.getToolWindow("search").orElse(null));
        assertSame(manager.getToolWindow("project").orElseThrow(),
                manager.getActive(ToolWindowPosition.LEFT));
    }

    @Test
    void swapContentSwitchesActiveWindow() {
        ToolWindowManager manager = newManager();
        manager.activate("project");

        manager.swapContent("project", "search");

        assertFalse(manager.getToolWindow("project").orElseThrow().isActive());
        assertSame(manager.getToolWindow("search").orElseThrow(),
                manager.getActive(ToolWindowPosition.LEFT));
    }

    @Test
    void swapContentRejectsDifferentPositions() {
        ToolWindowManager manager = newManager();
        manager.activate("project");
        assertThrows(IllegalStateException.class, () ->
                manager.swapContent("project", "terminal"));
    }

    @Test
    void swapContentWithUnknownWindowIsNoop() {
        ToolWindowManager manager = newManager();
        manager.activate("project");
        manager.swapContent("project", "unknown");
        assertSame(manager.getToolWindow("project").orElseThrow(),
                manager.getActive(ToolWindowPosition.LEFT));
    }

    @Test
    void getToolWindowsFiltersByPosition() {
        ToolWindowManager manager = newManager();
        assertEquals(2, manager.getToolWindows(ToolWindowPosition.LEFT).size());
        assertEquals(2, manager.getToolWindows(ToolWindowPosition.BOTTOM).size());
        assertEquals(4, manager.getAll().size());
    }

    @Test
    void activeListenersReceiveChanges() {
        ToolWindowManager manager = newManager();
        AtomicReference<ToolWindow> last = new AtomicReference<>();
        manager.addActiveToolWindowListener(last::set);

        manager.activate("project");
        assertSame(manager.getToolWindow("project").orElseThrow(), last.get());
    }

    @Test
    void activePositionListenersReceiveChanges() {
        ToolWindowManager manager = newManager();
        AtomicReference<ToolWindowPosition> last = new AtomicReference<>();
        manager.addActivePositionListener(last::set);

        manager.activate("terminal");
        assertEquals(ToolWindowPosition.BOTTOM, last.get());
    }

    @Test
    void activatePublishesEvent() {
        EventBus bus = new EventBus();
        ToolWindowManager manager = new ToolWindowManager(bus);
        manager.register("project", "Project", "PROJECT", ToolWindowPosition.LEFT);
        AtomicReference<ToolWindowActivatedEvent> captured = new AtomicReference<>();
        bus.subscribe(ToolWindowActivatedEvent.class, captured::set);

        manager.activate("project");

        assertEquals("project", captured.get().getToolWindowId());
        assertEquals(ToolWindowPosition.LEFT, captured.get().getPosition());
    }

    @Test
    void activateUnknownIdIsNoop() {
        ToolWindowManager manager = newManager();
        manager.activate("does-not-exist");
        assertNull(manager.getActive(ToolWindowPosition.LEFT));
    }
}
