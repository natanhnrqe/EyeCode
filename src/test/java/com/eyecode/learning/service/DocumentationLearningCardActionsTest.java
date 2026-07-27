package com.eyecode.learning.service;

import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.RelatedConcept;
import com.eyecode.learning.model.RelatedConceptNavigator;
import com.eyecode.learning.model.RelatedConceptResolver;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

    static final class RecordingExplainHandler implements ExplainMoreHandler {
        int calls = 0;
        final List<LearningConcept> explained = new ArrayList<>();

        @Override
        public void explain(LearningConcept concept) {
            calls++;
            explained.add(concept);
        }
    }

    static final class RecordingResolver implements RelatedConceptResolver {
        final java.util.Map<String, LearningConcept> table = new java.util.HashMap<>();

        @Override
        public Optional<LearningConcept> resolve(String id) {
            return Optional.ofNullable(table.get(id));
        }
    }

    private LearningConcept concept(String id, String title) {
        LearningConcept c = new LearningConcept();
        c.setId(id);
        c.setTitle(title);
        return c;
    }

    private DocumentationLearningCardActions makeActions(RecordingOpener opener,
                                                        RecordingExplainHandler explain,
                                                        RecordingResolver resolver,
                                                        LearningConcept concept,
                                                        List<RelatedConcept> related) {
        List<LearningConcept> navigated = new ArrayList<>();
        RelatedConceptNavigator navigator = new RelatedConceptNavigator(resolver, navigated::add);
        return new DocumentationLearningCardActions(opener, explain, navigator, concept, related);
    }

    @Test
    void constructorRejectsNullOpener() {
        assertThrows(NullPointerException.class,
                () -> new DocumentationLearningCardActions(null,
                        ExplainMoreHandler.delegatingTo(null),
                        new RelatedConceptNavigator(RelatedConceptResolver.empty(), c -> {}),
                        null, List.of()));
    }

    @Test
    void constructorRejectsNullExplainHandler() {
        assertThrows(NullPointerException.class,
                () -> new DocumentationLearningCardActions(new RecordingOpener(),
                        null,
                        new RelatedConceptNavigator(RelatedConceptResolver.empty(), c -> {}),
                        null, List.of()));
    }

    @Test
    void constructorRejectsNullNavigator() {
        assertThrows(NullPointerException.class,
                () -> new DocumentationLearningCardActions(new RecordingOpener(),
                        ExplainMoreHandler.delegatingTo(null),
                        null,
                        null, List.of()));
    }

    @Test
    void openDocumentationCallsOpenerWithConcept() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        LearningConcept concept = concept("class", "Class");
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, concept, List.of());
        actions.openDocumentation();
        assertEquals(1, opener.openCalls);
        assertEquals("Class", opener.opened.get(0).getTitle());
    }

    @Test
    void openDocumentationWithNullConceptIsNoOp() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, null, List.of());
        actions.openDocumentation();
        assertEquals(0, opener.openCalls);
    }

    @Test
    void explainMoreDelegatesToExplainHandler() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        LearningConcept concept = concept("class", "Class");
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, concept, List.of());
        actions.explainMore();
        assertEquals(1, explain.calls);
        assertEquals("Class", explain.explained.get(0).getTitle());
    }

    @Test
    void explainMoreWithNullConceptIsNoOp() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, null, List.of());
        actions.explainMore();
        assertEquals(0, explain.calls);
    }

    @Test
    void copyCodeNullIsNoOp() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, null, List.of());
        actions.copyCode(null);
        actions.copyCode("");
    }

    @Test
    void showRelatedConceptsEmptyIsNoOp() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, null, List.of());
        actions.showRelatedConcepts(List.of());
        actions.showRelatedConcepts(null);
    }

    @Test
    void showRelatedConceptsResolvesAndNavigates() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        LearningConcept b = concept("object", "Object");
        resolver.table.put("object", b);
        List<LearningConcept> navigated = new ArrayList<>();
        RelatedConceptNavigator navigator = new RelatedConceptNavigator(resolver, navigated::add);
        LearningConcept root = concept("class", "Class");
        List<RelatedConcept> related = List.of(RelatedConcept.of("object", "Object"));
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, explain, navigator, root, related);
        actions.showRelatedConcepts(related);
        assertEquals(1, navigated.size());
        assertEquals("object", navigated.get(0).getId());
    }

    @Test
    void showRelatedConceptsUnresolvableSkips() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        List<LearningConcept> navigated = new ArrayList<>();
        RelatedConceptNavigator navigator = new RelatedConceptNavigator(resolver, navigated::add);
        LearningConcept root = concept("class", "Class");
        List<RelatedConcept> related = List.of(RelatedConcept.of("missing", "Missing"));
        DocumentationLearningCardActions actions = new DocumentationLearningCardActions(opener, explain, navigator, root, related);
        actions.showRelatedConcepts(related);
        assertTrue(navigated.isEmpty());
    }

    @Test
    void relatedConceptsAccessorsReflectInput() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        List<RelatedConcept> related = List.of(
                RelatedConcept.of("a", "A"),
                RelatedConcept.of("b", "B")
        );
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, null, related);
        assertEquals(2, actions.relatedConcepts().size());
        assertTrue(actions.hasRelatedConcepts());
    }

    @Test
    void nullRelatedConceptsListBecomesEmpty() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, null, null);
        assertFalse(actions.hasRelatedConcepts());
        assertTrue(actions.relatedConcepts().isEmpty());
    }

    @Test
    void hasConceptFalseWhenConceptNull() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, null, List.of());
        assertFalse(actions.hasConcept());
    }

    @Test
    void conceptAccessorReturnsCurrent() {
        RecordingOpener opener = new RecordingOpener();
        RecordingExplainHandler explain = new RecordingExplainHandler();
        RecordingResolver resolver = new RecordingResolver();
        LearningConcept concept = concept("class", "Class");
        DocumentationLearningCardActions actions = makeActions(opener, explain, resolver, concept, List.of());
        assertEquals("class", actions.concept().getId());
    }
}
