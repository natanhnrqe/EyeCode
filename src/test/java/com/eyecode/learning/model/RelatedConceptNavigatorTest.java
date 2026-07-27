package com.eyecode.learning.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class RelatedConceptNavigatorTest {

    static final class StubResolver implements RelatedConceptResolver {
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

    @Test
    void constructorRejectsNullResolver() {
        assertThrows(NullPointerException.class,
                () -> new RelatedConceptNavigator(null, c -> {}));
    }

    @Test
    void constructorRejectsNullCallback() {
        assertThrows(NullPointerException.class,
                () -> new RelatedConceptNavigator(RelatedConceptResolver.empty(), null));
    }

    @Test
    void navigateToResolvesAndInvokesCallback() {
        StubResolver resolver = new StubResolver();
        LearningConcept target = concept("object", "Object");
        resolver.table.put("object", target);
        List<LearningConcept> navigated = new ArrayList<>();
        RelatedConceptNavigator nav = new RelatedConceptNavigator(resolver, navigated::add);
        boolean result = nav.navigateTo(RelatedConcept.of("object", "Object"));
        assertTrue(result);
        assertEquals(1, navigated.size());
        assertEquals("object", navigated.get(0).getId());
    }

    @Test
    void navigateToUnresolvableReturnsFalse() {
        StubResolver resolver = new StubResolver();
        List<LearningConcept> navigated = new ArrayList<>();
        RelatedConceptNavigator nav = new RelatedConceptNavigator(resolver, navigated::add);
        boolean result = nav.navigateTo(RelatedConcept.of("missing", "Missing"));
        assertFalse(result);
        assertTrue(navigated.isEmpty());
    }

    @Test
    void navigateToNullConceptReturnsFalse() {
        StubResolver resolver = new StubResolver();
        List<LearningConcept> navigated = new ArrayList<>();
        RelatedConceptNavigator nav = new RelatedConceptNavigator(resolver, navigated::add);
        assertFalse(nav.navigateTo(null));
        assertTrue(navigated.isEmpty());
    }

    @Test
    void navigateToBlankIdReturnsFalse() {
        StubResolver resolver = new StubResolver();
        resolver.table.put("", concept("", ""));
        List<LearningConcept> navigated = new ArrayList<>();
        RelatedConceptNavigator nav = new RelatedConceptNavigator(resolver, navigated::add);
        assertFalse(nav.navigateTo(RelatedConcept.of("", "")));
        assertFalse(nav.navigateTo(RelatedConcept.of("   ", "x")));
    }

    @Test
    void emptyResolverReturnsFalse() {
        RelatedConceptResolver empty = RelatedConceptResolver.empty();
        List<LearningConcept> navigated = new ArrayList<>();
        RelatedConceptNavigator nav = new RelatedConceptNavigator(empty, navigated::add);
        assertFalse(nav.navigateTo(RelatedConcept.of("class", "Class")));
    }
}
