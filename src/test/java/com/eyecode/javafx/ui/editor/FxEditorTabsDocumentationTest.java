package com.eyecode.javafx.ui.editor;

import javafx.application.Platform;
import javafx.scene.control.Tab;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FxEditorTabsDocumentationTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void documentationSharesTheNormalTabStripWithoutAFileSession() throws Exception {
        runInFx(() -> {
            FxEditorTabs tabs = new FxEditorTabs();
            tabs.update(List.of(
                    new TabModel("animal", "Animal.java", false, false, false),
                    new TabModel("shape", "Shape.java", false, false, false)),
                    "animal");
            JavaFxDocumentationTab content = new JavaFxDocumentationTab(
                    new JavaFxDocumentationSurface((url, policy) -> null));
            tabs.addDocumentationTab(content, () -> { });
            tabs.addDocumentationTab(content, () -> { });

            assertEquals(3, tabs.getTabs().size());
            Tab documentation = tabs.getTabs().get(2);
            assertEquals("Documentation", documentation.getText());
            assertEquals(JavaFxDocumentationWorkspace.TAB_ID, documentation.getUserData());
            tabs.showDocumentation();
            assertTrue(tabs.getSelectionModel().isSelected(2));
        });
    }

    private static void runInFx(Runnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        Platform.runLater(() -> {
            try {
                action.run();
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
    }
}
