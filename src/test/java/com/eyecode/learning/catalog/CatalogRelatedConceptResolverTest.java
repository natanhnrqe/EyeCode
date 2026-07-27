package com.eyecode.learning.catalog;

import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.LearningConcept;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class CatalogRelatedConceptResolverTest {

    @Test
    void constructorRejectsNullCatalog() {
        assertThrows(NullPointerException.class, () -> new CatalogRelatedConceptResolver(null));
    }

    @Test
    void resolvesValidId() {
        LearningCatalog catalog = new DefaultLearningCatalog();
        CatalogRelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        Optional<LearningConcept> resolved = resolver.resolve("class");
        assertTrue(resolved.isPresent());
        assertEquals("Class", resolved.get().getTitle());
        assertEquals(ConceptType.CLASS, resolved.get().getType());
    }

    @Test
    void resolvesIdCaseInsensitive() {
        LearningCatalog catalog = new DefaultLearningCatalog();
        CatalogRelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        Optional<LearningConcept> resolved = resolver.resolve("INTERFACE");
        assertTrue(resolved.isPresent());
        assertEquals("Interface", resolved.get().getTitle());
    }

    @Test
    void resolvesAllRegisteredConcepts() {
        LearningCatalog catalog = new DefaultLearningCatalog();
        CatalogRelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        assertTrue(resolver.resolve("class").isPresent());
        assertTrue(resolver.resolve("interface").isPresent());
        assertTrue(resolver.resolve("enum").isPresent());
        assertTrue(resolver.resolve("record").isPresent());
        assertTrue(resolver.resolve("object").isPresent());
    }

    @Test
    void unknownIdReturnsEmpty() {
        LearningCatalog catalog = new DefaultLearningCatalog();
        CatalogRelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        assertTrue(resolver.resolve("missing").isEmpty());
    }

    @Test
    void nullIdReturnsEmpty() {
        LearningCatalog catalog = new DefaultLearningCatalog();
        CatalogRelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        assertTrue(resolver.resolve(null).isEmpty());
    }

    @Test
    void blankIdReturnsEmpty() {
        LearningCatalog catalog = new DefaultLearningCatalog();
        CatalogRelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        assertTrue(resolver.resolve("").isEmpty());
        assertTrue(resolver.resolve("   ").isEmpty());
    }

    @Test
    void allConceptsExposes5Registered() {
        LearningCatalog catalog = new DefaultLearningCatalog();
        assertEquals(5, catalog.allConcepts().size());
    }

    @Test
    void defaultCatalogPopulatesRelatedConcepts() {
        LearningCatalog catalog = new DefaultLearningCatalog();
        LearningConcept classConcept = catalog.get(ConceptType.CLASS);
        assertNotNull(classConcept.getRelatedConcepts());
        assertEquals(3, classConcept.getRelatedConcepts().size());
        assertTrue(classConcept.getRelatedConcepts().contains("object"));
    }
}
