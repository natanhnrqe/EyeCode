package com.eyecode.learning.catalog;

import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.content.LearningDepth;
import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSyntaxLearningCatalogTest {

    @Test
    void everyIndexedConceptHasValidContentAndUniqueTrigger() {
        JavaSyntaxLearningCatalog catalog = new JavaSyntaxLearningCatalog();
        LearningContentEngine engine = new LearningContentEngine();
        var triggers = new HashSet<String>();

        for (var concept : catalog.allConcepts()) {
            assertTrue(triggers.add(concept.getTrigger()));
            assertNotNull(concept.getTitle());
            var document = engine.loadDocument(concept.getPage().getId());
            assertEquals(concept.getPage().getId(), document.metadata().id());
            assertFalse(document.markdownBody().isBlank());
            for (String related : document.metadata().related()) {
                assertFalse(engine.loadDocument(related).markdownBody().isBlank(), related);
            }
        }
    }

    @Test
    void quickAndFullDepthsArePreserved() {
        LearningContentEngine engine = new LearningContentEngine();

        assertEquals(LearningDepth.QUICK,
                engine.loadDocument("java/syntax/modifiers/final").metadata().depth());
        assertEquals(LearningDepth.FULL,
                engine.loadDocument("java/types/class").metadata().depth());
    }
}
