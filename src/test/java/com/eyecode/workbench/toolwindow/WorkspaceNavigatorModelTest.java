package com.eyecode.workbench.toolwindow;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkspaceNavigatorModelTest {

    private WorkspaceNavigatorItem item(String id) {
        return new WorkspaceNavigatorItem(id, "PROJECT", id, id, id);
    }

    @Test
    void validatesIdAndTarget() {
        assertThrows(IllegalArgumentException.class, () ->
                new WorkspaceNavigatorItem("  ", "ICON", "Titulo", "Tip", "target"));
        assertThrows(IllegalArgumentException.class, () ->
                new WorkspaceNavigatorItem("id", "ICON", "Titulo", "Tip", "  "));
    }

    @Test
    void setItemsMakesFirstItemActive() {
        WorkspaceNavigatorModel model = new WorkspaceNavigatorModel();
        model.setItems(List.of(item("project"), item("search")));

        assertEquals(2, model.getItems().size());
        assertEquals("project", model.getActiveItem().getId());
    }

    @Test
    void selectChangesActiveItem() {
        WorkspaceNavigatorModel model = new WorkspaceNavigatorModel();
        model.setItems(List.of(item("project"), item("search")));

        model.select("search");

        assertEquals("search", model.getActiveItem().getId());
    }

    @Test
    void selectUnknownIdIsNoop() {
        WorkspaceNavigatorModel model = new WorkspaceNavigatorModel();
        model.setItems(List.of(item("project")));

        model.select("unknown");

        assertEquals("project", model.getActiveItem().getId());
    }

    @Test
    void findItemLocatesById() {
        WorkspaceNavigatorModel model = new WorkspaceNavigatorModel();
        model.setItems(List.of(item("project"), item("search")));

        assertTrue(model.findItem("search").isPresent());
        assertFalse(model.findItem("missing").isPresent());
        assertFalse(model.findItem(null).isPresent());
    }

    @Test
    void emptyModelHasNoActiveItem() {
        WorkspaceNavigatorModel model = new WorkspaceNavigatorModel();
        model.setItems(List.of());
        assertNull(model.getActiveItem());
    }

    @Test
    void selectionListenerNotified() {
        WorkspaceNavigatorModel model = new WorkspaceNavigatorModel();
        model.setItems(List.of(item("project"), item("search")));
        AtomicReference<WorkspaceNavigatorItem> last = new AtomicReference<>();
        model.addSelectionListener(last::set);

        model.select("search");

        assertEquals("search", last.get().getId());
    }

    @Test
    void changeListenerNotifiedOnSetAndSelect() {
        WorkspaceNavigatorModel model = new WorkspaceNavigatorModel();
        AtomicInteger changes = new AtomicInteger();
        model.addChangeListener(changes::incrementAndGet);

        model.setItems(List.of(item("project"), item("search")));
        model.select("search");

        assertEquals(2, changes.get());
    }

    @Test
    void itemExposesAllFields() {
        WorkspaceNavigatorItem i = new WorkspaceNavigatorItem("learn", "FOLDERS", "Learn", "Aprenda", "learn");
        assertEquals("learn", i.getId());
        assertEquals("FOLDERS", i.getIconKey());
        assertEquals("Learn", i.getTitle());
        assertEquals("Aprenda", i.getTooltip());
        assertEquals("learn", i.getTargetToolWindowId());
    }
}
