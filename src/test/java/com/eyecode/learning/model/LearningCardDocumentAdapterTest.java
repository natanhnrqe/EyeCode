package com.eyecode.learning.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LearningCardDocumentAdapterTest {

    @Test
    void nullConceptReturnsEmptyDocument() {
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(null);
        assertNotNull(doc);
        assertTrue(doc.getBlocks().isEmpty());
    }

    @Test
    void relatedConceptsFromNullConcept() {
        List<RelatedConcept> related = LearningCardDocumentAdapter.relatedConceptsFrom(null);
        assertNotNull(related);
        assertTrue(related.isEmpty());
    }

    @Test
    void relatedConceptsFromEmptyList() {
        LearningConcept concept = new LearningConcept();
        concept.setRelatedConcepts(List.of());
        List<RelatedConcept> related = LearningCardDocumentAdapter.relatedConceptsFrom(concept);
        assertTrue(related.isEmpty());
    }

    @Test
    void relatedConceptsConvertsToRelatedConcept() {
        LearningConcept concept = new LearningConcept();
        concept.setRelatedConcepts(List.of("Inheritance", "Polymorphism"));
        List<RelatedConcept> related = LearningCardDocumentAdapter.relatedConceptsFrom(concept);
        assertEquals(2, related.size());
        assertEquals("Inheritance", related.get(0).title());
        assertEquals("Polymorphism", related.get(1).title());
    }

    @Test
    void adapterPreservesTitleAndDescription() {
        LearningConcept concept = new LearningConcept();
        concept.setTitle("Lambda");
        concept.setDescription("A lambda is an anonymous function.");
        concept.setType(ConceptType.CLASS);
        concept.setRelatedConcepts(List.of("Method Reference"));
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        assertEquals("Lambda", doc.getHeader().title());
        assertTrue(doc.getBlocks().size() > 2);
        assertTrue(doc.getCodeBlocks().size() >= 1);
        assertEquals(1, doc.getRelatedConcepts().size());
        assertEquals("Method Reference", doc.getRelatedConcepts().get(0).title());
    }

    @Test
    void adapterProducesCodeBlockWhenTypePresent() {
        LearningConcept concept = new LearningConcept();
        concept.setTitle("Lambda");
        concept.setDescription("Anonymous function");
        concept.setType(ConceptType.CLASS);
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        List<LearningCardBlock.CodeBlock> codes = doc.getCodeBlocks();
        assertEquals(1, codes.size());
        assertTrue(codes.get(0).code().contains("class Lambda"));
    }

    @Test
    void adapterSkipsCodeBlockWhenTypeAbsent() {
        LearningConcept concept = new LearningConcept();
        concept.setTitle("Lambda");
        concept.setDescription("Anonymous function");
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        assertTrue(doc.getCodeBlocks().isEmpty());
    }

    @Test
    void adapterRelatedConceptsPopulatedInDocument() {
        LearningConcept concept = new LearningConcept();
        concept.setTitle("Class");
        concept.setDescription("desc");
        concept.setType(ConceptType.CLASS);
        concept.setRelatedConcepts(List.of("object", "interface"));
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        assertEquals(2, doc.getRelatedConcepts().size());
        assertEquals("object", doc.getRelatedConcepts().get(0).title());
    }

    @Test
    void adapterHandlesEmptyDescription() {
        LearningConcept concept = new LearningConcept();
        concept.setTitle("Class");
        concept.setType(ConceptType.CLASS);
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        assertFalse(doc.getBlocks().stream()
                .anyMatch(b -> b instanceof LearningCardBlock.ParagraphBlock p && p.text().isBlank()));
    }

    @Test
    void adapterHandlesEmptyTitle() {
        LearningConcept concept = new LearningConcept();
        concept.setDescription("desc only");
        concept.setType(ConceptType.CLASS);
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        assertEquals("", doc.getHeader().title());
        assertTrue(doc.getCodeBlocks().size() == 1);
    }

    @Test
    void adapterSkipsBlankRelatedConcepts() {
        LearningConcept concept = new LearningConcept();
        concept.setType(ConceptType.CLASS);
        concept.setRelatedConcepts(java.util.Arrays.asList("class", "", "  ", null));
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        assertEquals(1, doc.getRelatedConcepts().size());
        assertEquals("class", doc.getRelatedConcepts().get(0).title());
    }

    @Test
    void adapterUsesRealMarkdownWhenPageExists() {
        LearningConcept concept = new LearningConcept();
        concept.setTitle("Class");
        concept.setType(ConceptType.CLASS);
        concept.setRelatedConcepts(List.of());
        LearningConcept realConcept = new com.eyecode.learning.catalog.DefaultLearningCatalog().get(ConceptType.CLASS);
        assertNotNull(realConcept);
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(realConcept);
        assertNotNull(doc.getHeader());
        assertNotNull(doc.getFooter());
        assertTrue(doc.getBlocks().size() > 0 || doc.getRelatedConcepts().size() > 0);
        List<LearningCardBlock.CodeBlock> codes = doc.getCodeBlocks();
        assertTrue(codes.isEmpty() || codes.size() == 1);
        if (!codes.isEmpty()) {
            assertTrue(codes.get(0).code().contains("public class") || !codes.get(0).language().isBlank());
        }
    }
}
