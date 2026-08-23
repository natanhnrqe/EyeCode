package com.eyecode.javafx.ui;

import javafx.application.Platform;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxToolbarMenuTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void hamburgerContainsProjectLifecycleActionsWithoutPermanentToolbarButtons() {
        FxToolbar toolbar = new FxToolbar();
        toolbar.setProjectMenuActions(() -> { }, () -> { }, () -> { });

        ContextMenu menu = toolbar.projectMenuForTest();
        assertNotNull(menu);
        assertEquals("Project / File", menu.getItems().getFirst().getText());
        assertEquals("New Project", menu.getItems().get(1).getText());
        assertEquals("Open Project", menu.getItems().get(2).getText());
        assertEquals("Recent Projects", menu.getItems().get(3).getText());
        assertEquals(4, ((javafx.scene.layout.HBox) toolbar.getChildren().getFirst()).getChildren().size());
    }

    @Test
    void toolbarExecutionButtonsRemainFlatIconControls() {
        FxToolbar toolbar = new FxToolbar();

        assertEquals(3, toolbar.executionButtonsForTest().size());
        toolbar.executionButtonsForTest().forEach(button ->
                assertFalse(button.getStyleClass().contains("eyecode-button")));
    }

    @Test
    void executionButtonsDelegateToConfiguredActions() {
        FxToolbar toolbar = new FxToolbar();
        AtomicInteger run = new AtomicInteger();
        AtomicInteger rerun = new AtomicInteger();
        AtomicInteger stop = new AtomicInteger();
        AtomicBoolean running = new AtomicBoolean();
        toolbar.setExecutionActions(run::incrementAndGet, rerun::incrementAndGet,
                stop::incrementAndGet, running::get, () -> true);

        List<Button> buttons = toolbar.executionButtonsForTest();
        buttons.get(0).fire();
        buttons.get(1).fire();
        running.set(true);
        toolbar.refreshExecutionState();
        buttons.get(2).fire();

        assertEquals(1, run.get());
        assertEquals(1, rerun.get());
        assertEquals(1, stop.get());
    }
}
