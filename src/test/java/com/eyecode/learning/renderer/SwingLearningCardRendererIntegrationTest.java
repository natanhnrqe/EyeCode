package com.eyecode.learning.renderer;

import com.eyecode.learning.browser.LearningChromiumCard;
import com.eyecode.learning.catalog.CatalogRelatedConceptResolver;
import com.eyecode.learning.catalog.DefaultLearningCatalog;
import com.eyecode.learning.catalog.LearningCatalog;
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

class SwingLearningCardRendererIntegrationTest {

    static final class RecordingOpener implements DocumentationOpener {
        int calls = 0;
        final List<LearningConcept> opened = new ArrayList<>();

        @Override
        public void open(LearningConcept concept) {
            calls++;
            opened.add(concept);
        }
    }

    @Test
    void endToEndNavigateClassToObjectViaRelatedConcepts() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        LearningCatalog catalog = new DefaultLearningCatalog();
        RelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        RecordingOpener opener = new RecordingOpener();

        SwingLearningCardRenderer renderer = SwingLearningCardRenderer.withOpener(
                opener, resolver, ExplainMoreHandler.delegatingTo(opener));

        List<LearningConcept> auxCaptured = new ArrayList<>();
        SwingLearningCardRenderer auxRef = renderer;

        SwingUtilities.invokeAndWait(() -> {
            LearningConcept classConcept = catalog.get(ConceptType.CLASS);
            assertNotNull(classConcept);
            assertEquals(3, classConcept.getRelatedConcepts().size());
            assertTrue(classConcept.getRelatedConcepts().contains("object"));

            renderer.show(classConcept);
            assertTrue(renderer.isVisible());

            renderer.currentActionsForTest().showRelatedConcepts(
                    com.eyecode.learning.model.LearningCardDocumentAdapter
                            .relatedConceptsFrom(classConcept));
        });

        Thread.sleep(50);

        SwingUtilities.invokeAndWait(() -> {
            try {
                assertTrue(renderer.isVisible());
            } finally {
                renderer.dispose();
            }
        });
    }

    @Test
    void endToEndContextReplacementFromAToB() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        LearningCatalog catalog = new DefaultLearningCatalog();
        RelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        RecordingOpener opener = new RecordingOpener();
        SwingLearningCardRenderer renderer = SwingLearningCardRenderer.withOpener(
                opener, resolver, ExplainMoreHandler.delegatingTo(opener));

        SwingUtilities.invokeAndWait(() -> {
            LearningConcept classConcept = catalog.get(ConceptType.CLASS);
            renderer.show(classConcept);
            renderer.currentActionsForTest().openDocumentation();
            assertEquals(1, opener.calls);
            assertEquals("Class", opener.opened.get(0).getTitle());

            LearningConcept objectConcept = catalog.get(ConceptType.OBJECT);
            renderer.update(objectConcept);
            renderer.currentActionsForTest().openDocumentation();
            assertEquals(2, opener.calls);
            assertEquals("Object", opener.opened.get(1).getTitle());
        });
        SwingUtilities.invokeAndWait(renderer::dispose);
    }

    @Test
    void endToEndRendererReusedAcrossThreeConcepts() throws Exception {
        LearningChromiumCard.USE_CEF = false;
        LearningCatalog catalog = new DefaultLearningCatalog();
        RelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        RecordingOpener opener = new RecordingOpener();
        SwingLearningCardRenderer renderer = SwingLearningCardRenderer.withOpener(
                opener, resolver, ExplainMoreHandler.delegatingTo(opener));

        SwingUtilities.invokeAndWait(() -> {
            for (ConceptType type : new ConceptType[]{ConceptType.CLASS, ConceptType.INTERFACE, ConceptType.ENUM}) {
                LearningConcept concept = catalog.get(type);
                if (concept == null) continue;
                renderer.update(concept);
                assertTrue(renderer.isVisible());
                renderer.currentActionsForTest().openDocumentation();
            }
            assertEquals(3, opener.calls);
            assertEquals("Class", opener.opened.get(0).getTitle());
            assertEquals("Interface", opener.opened.get(1).getTitle());
            assertEquals("Enum", opener.opened.get(2).getTitle());
        });
        SwingUtilities.invokeAndWait(renderer::dispose);
    }
}
