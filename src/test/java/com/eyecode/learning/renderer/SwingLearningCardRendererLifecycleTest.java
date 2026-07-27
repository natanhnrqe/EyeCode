package com.eyecode.learning.renderer;

import com.eyecode.learning.browser.LearningChromiumCard;
import com.eyecode.learning.model.LearningConcept;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwingLearningCardRendererLifecycleTest {

    private LearningConcept concept(String title, String description, List<String> related) {
        LearningConcept c = new LearningConcept();
        c.setTitle(title);
        c.setDescription(description);
        c.setRelatedConcepts(related);
        return c;
    }

    @Test
    void lifecycleShowUpdateHideShowDisposeNoException() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        SwingLearningCardRenderer renderer = new SwingLearningCardRenderer();
        SwingUtilities.invokeAndWait(() -> {
            try {
                LearningConcept a = concept("Class", "A", List.of("Inheritance"));
                renderer.show(a);
                renderer.update(concept("Lambda", "B", List.of("MethodRef")));
                renderer.hide();
                renderer.show(concept("Stream", "C", List.of()));
                renderer.dispose();
                assertFalse(renderer.isVisible());
            } catch (Throwable t) {
                fail("Lifecycle threw: " + t.getMessage(), t);
            }
        });
    }

    @Test
    void showWithNullConceptDoesNotThrow() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        SwingLearningCardRenderer renderer = new SwingLearningCardRenderer();
        SwingUtilities.invokeAndWait(() -> {
            try {
                renderer.show(null);
                renderer.hide();
                renderer.dispose();
            } catch (Throwable t) {
                fail("show(null) threw: " + t.getMessage(), t);
            }
        });
    }

    @Test
    void disposeIsIdempotent() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        SwingLearningCardRenderer renderer = new SwingLearningCardRenderer();
        SwingUtilities.invokeAndWait(() -> {
            try {
                renderer.dispose();
                renderer.dispose();
            } catch (Throwable t) {
                fail("Double dispose threw: " + t.getMessage(), t);
            }
        });
    }

    @Test
    void updateBeforeShowTriggersShowFlow() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        SwingLearningCardRenderer renderer = new SwingLearningCardRenderer();
        SwingUtilities.invokeAndWait(() -> {
            try {
                LearningConcept a = concept("Class", "A", List.of());
                renderer.update(a);
                assertTrue(renderer.isVisible());
                renderer.dispose();
            } catch (Throwable t) {
                fail("update before show threw: " + t.getMessage(), t);
            }
        });
    }

    @Test
    void hideAfterDisposeDoesNotThrow() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        SwingLearningCardRenderer renderer = new SwingLearningCardRenderer();
        SwingUtilities.invokeAndWait(() -> {
            try {
                LearningConcept a = concept("Class", "A", List.of());
                renderer.show(a);
                renderer.dispose();
                renderer.hide();
            } catch (Throwable t) {
                fail("hide after dispose threw: " + t.getMessage(), t);
            }
        });
    }
}
