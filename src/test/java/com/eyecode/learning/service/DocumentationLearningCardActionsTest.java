package com.eyecode.learning.service;

import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.RelatedConcept;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentationLearningCardActionsTest {

    static final class RecordingOpener implements DocumentationOpener {
        int openCalls = 0;
        final List<LearningConcept> opened = new ArrayList<>();

        @Override
        public void open(LearningConcept concept) {
            openCalls++;
            opened.add(concept);
        }
    }

    @Test
    void constructorRejectsNullOpener() {
        assertThrows(NullPointerException.class,
                () -> new DocumentationLearningCardActions(null, null, List.of()));
    }

    @Test
    void openDocumentationCallsOpenerWithConcept() {
        RecordingOpener opener = new RecordingOpener();
        LearningConcept concept = new LearningConcept();
        concept.setTitle("Stream");
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, concept, List.of());
        actions.openDocumentation();
        assertEquals(1, opener.openCalls);
        assertEquals("Stream", opener.opened.get(0).getTitle());
    }

    @Test
    void openDocumentationWithNullConceptIsNoOp() {
        RecordingOpener opener = new RecordingOpener();
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, null, List.of());
        actions.openDocumentation();
        assertEquals(0, opener.openCalls);
    }

    @Test
    void explainMoreDelegatesToOpener() {
        RecordingOpener opener = new RecordingOpener();
        LearningConcept concept = new LearningConcept();
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, concept, List.of());
        actions.explainMore();
        assertEquals(1, opener.openCalls);
        assertEquals(concept, opener.opened.get(0));
    }

    @Test
    void explainMoreWithNullConceptIsNoOp() {
        RecordingOpener opener = new RecordingOpener();
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, null, List.of());
        actions.explainMore();
        assertEquals(0, opener.openCalls);
    }

    @Test
    void copyCodeNullIsNoOp() {
        RecordingOpener opener = new RecordingOpener();
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, null, List.of());
        actions.copyCode(null);
        actions.copyCode("");
    }

    @Test
    void showRelatedConceptsEmptyIsNoOp() {
        RecordingOpener opener = new RecordingOpener();
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, null, List.of());
        actions.showRelatedConcepts(List.of());
        actions.showRelatedConcepts(null);
    }

    @Test
    void relatedConceptsAccessorsReflectInput() {
        RecordingOpener opener = new RecordingOpener();
        List<RelatedConcept> related = List.of(
                RelatedConcept.of("a", "A"),
                RelatedConcept.of("b", "B")
        );
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, null, related);
        assertEquals(2, actions.relatedConcepts().size());
        assertTrue(actions.hasRelatedConcepts());
    }

    @Test
    void nullRelatedConceptsListBecomesEmpty() {
        RecordingOpener opener = new RecordingOpener();
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, null, null);
        assertFalse(actions.hasRelatedConcepts());
        assertTrue(actions.relatedConcepts().isEmpty());
    }

    @Test
    void hasConceptFalseWhenConceptNull() {
        RecordingOpener opener = new RecordingOpener();
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, null, List.of());
        assertFalse(actions.hasConcept());
    }
}
