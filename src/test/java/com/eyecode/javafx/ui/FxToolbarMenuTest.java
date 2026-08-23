package com.eyecode.javafx.ui;

import javafx.application.Platform;
import javafx.scene.control.ContextMenu;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
