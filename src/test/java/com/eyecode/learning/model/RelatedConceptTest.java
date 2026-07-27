package com.eyecode.learning.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RelatedConceptTest {

    @Test
    void ofCreatesWithIdAndTitle() {
        RelatedConcept rc = RelatedConcept.of("inh", "Inheritance");
        assertEquals("inh", rc.id());
        assertEquals("Inheritance", rc.title());
    }

    @Test
    void fromTitleUsesTitleAsId() {
        RelatedConcept rc = RelatedConcept.fromTitle("Encapsulation");
        assertEquals("Encapsulation", rc.id());
        assertEquals("Encapsulation", rc.title());
    }

    @Test
    void ofNullIdBecomesEmptyString() {
        RelatedConcept rc = RelatedConcept.of(null, "Title");
        assertEquals("", rc.id());
        assertEquals("Title", rc.title());
    }

    @Test
    void ofNullTitleBecomesEmptyString() {
        RelatedConcept rc = RelatedConcept.of("id", null);
        assertEquals("id", rc.id());
        assertEquals("", rc.title());
    }
}
