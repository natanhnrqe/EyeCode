package com.eyecode.learning.renderer;

import com.eyecode.learning.browser.LearningChromiumCard;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.RelatedConceptResolver;
import com.eyecode.learning.service.DocumentationOpener;
import com.eyecode.learning.service.ExplainMoreHandler;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwingLearningCardRendererDocumentationTest {

    static final class RecordingOpener implements DocumentationOpener {
        int calls = 0;
        final List<LearningConcept> opened = new ArrayList<>();

        @Override
        public void open(LearningConcept concept) {
            calls++;
            opened.add(concept);
        }
    }

    private LearningConcept concept(String id, String title, ConceptType type) {
        LearningConcept c = new LearningConcept();
        c.setId(id);
        c.setTitle(title);
        c.setDescription("desc for " + title);
        c.setType(type);
        return c;
    }

    private SwingLearningCardRenderer rendererWith(RecordingOpener opener) {
        return SwingLearningCardRenderer.withOpener(opener,
                RelatedConceptResolver.empty(),
                ExplainMoreHandler.delegatingTo(opener));
    }

    @Test
    void openDocumentationButtonCallsOpenerWithFirstConcept() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        RecordingOpener opener = new RecordingOpener();
        SwingLearningCardRenderer renderer = rendererWith(opener);
        SwingUtilities.invokeAndWait(() -> {
            LearningConcept a = concept("class", "Class", ConceptType.CLASS);
            renderer.show(a);
            renderer.currentActionsForTest().openDocumentation();
        });
        assertEquals(1, opener.calls);
        assertEquals("Class", opener.opened.get(0).getTitle());
        SwingUtilities.invokeAndWait(renderer::dispose);
    }

    @Test
    void explainMoreButtonCallsExplainHandler() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        RecordingOpener opener = new RecordingOpener();
        SwingLearningCardRenderer renderer = rendererWith(opener);
        SwingUtilities.invokeAndWait(() -> {
            LearningConcept a = concept("class", "Class", ConceptType.CLASS);
            renderer.show(a);
            renderer.currentActionsForTest().explainMore();
        });
        assertEquals(1, opener.calls);
        SwingUtilities.invokeAndWait(renderer::dispose);
    }

    @Test
    void firstSecondOpensBoth() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        RecordingOpener opener = new RecordingOpener();
        SwingLearningCardRenderer renderer = rendererWith(opener);
        SwingUtilities.invokeAndWait(() -> {
            LearningConcept a = concept("class", "Class", ConceptType.CLASS);
            renderer.show(a);
            renderer.currentActionsForTest().openDocumentation();

            LearningConcept b = concept("interface", "Interface", ConceptType.INTERFACE);
            renderer.update(b);
            renderer.currentActionsForTest().openDocumentation();
        });
        assertEquals(2, opener.calls);
        assertEquals("Class", opener.opened.get(0).getTitle());
        assertEquals("Interface", opener.opened.get(1).getTitle());
        SwingUtilities.invokeAndWait(renderer::dispose);
    }

    @Test
    void navigationAtoBDocumentationUsesB() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        RecordingOpener opener = new RecordingOpener();
        SwingLearningCardRenderer renderer = rendererWith(opener);
        SwingUtilities.invokeAndWait(() -> {
            LearningConcept a = concept("class", "Class", ConceptType.CLASS);
            a.setRelatedConcepts(List.of("object"));
            renderer.show(a);
            renderer.currentActionsForTest().openDocumentation();
            assertEquals(1, opener.calls);
            assertEquals("Class", opener.opened.get(0).getTitle());

            LearningConcept b = concept("object", "Object", ConceptType.OBJECT);
            renderer.update(b);
            renderer.currentActionsForTest().openDocumentation();
            assertEquals(2, opener.calls);
            assertEquals("Object", opener.opened.get(1).getTitle());
        });
        SwingUtilities.invokeAndWait(renderer::dispose);
    }

    @Test
    void openerNullConceptNoOp() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        RecordingOpener opener = new RecordingOpener();
        SwingLearningCardRenderer renderer = rendererWith(opener);
        SwingUtilities.invokeAndWait(() -> {
            renderer.show(null);
            renderer.currentActionsForTest().openDocumentation();
        });
        assertEquals(0, opener.calls);
        SwingUtilities.invokeAndWait(renderer::dispose);
    }

    @Test
    void noConceptsResolvableEmptyResolver() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        RecordingOpener opener = new RecordingOpener();
        SwingLearningCardRenderer renderer = rendererWith(opener);
        SwingUtilities.invokeAndWait(() -> {
            LearningConcept a = concept("class", "Class", ConceptType.CLASS);
            a.setRelatedConcepts(List.of("missing"));
            renderer.show(a);
            renderer.currentActionsForTest().showRelatedConcepts(
                    com.eyecode.learning.model.LearningCardDocumentAdapter.relatedConceptsFrom(a));
        });
        assertTrue(renderer.isVisible());
        SwingUtilities.invokeAndWait(renderer::dispose);
    }
}
