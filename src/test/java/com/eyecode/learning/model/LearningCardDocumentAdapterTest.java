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
        concept.setRelatedConcepts(List.of("Method Reference"));
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        assertEquals("Lambda", doc.getHeader().title());
        assertTrue(doc.getBlocks().size() > 2);
        assertTrue(doc.getCodeBlocks().size() >= 1);
    }

    @Test
    void adapterProducesCodeBlockWhenTitlePresent() {
        LearningConcept concept = new LearningConcept();
        concept.setTitle("Lambda");
        concept.setDescription("Anonymous function");
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        List<LearningCardBlock.CodeBlock> codes = doc.getCodeBlocks();
        assertEquals(1, codes.size());
        assertTrue(codes.get(0).code().contains("class Lambda"));
    }
}
