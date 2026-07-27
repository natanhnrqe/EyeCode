package com.eyecode.learning.service;

import com.eyecode.learning.model.LearningConcept;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ExplainMoreHandlerTest {

    static final class RecordingOpener implements DocumentationOpener {
        int calls = 0;
        final List<LearningConcept> opened = new ArrayList<>();

        @Override
        public void open(LearningConcept concept) {
            calls++;
            opened.add(concept);
        }
    }

    private LearningConcept concept(String title) {
        LearningConcept c = new LearningConcept();
        c.setTitle(title);
        return c;
    }

    @Test
    void delegatingToNullReturnsNoop() {
        ExplainMoreHandler handler = ExplainMoreHandler.delegatingTo(null);
        assertInstanceOf(ExplainMoreHandler.NoopExplainMoreHandler.class, handler);
        assertDoesNotThrow(() -> handler.explain(concept("Class")));
    }

    @Test
    void delegatingToOpenerCallsOpener() {
        RecordingOpener opener = new RecordingOpener();
        ExplainMoreHandler handler = ExplainMoreHandler.delegatingTo(opener);
        assertInstanceOf(ExplainMoreHandler.DocumentationExplainMoreHandler.class, handler);
        LearningConcept concept = concept("Class");
        handler.explain(concept);
        assertEquals(1, opener.calls);
        assertEquals("Class", opener.opened.get(0).getTitle());
    }

    @Test
    void delegatingHandlerWithNullConceptIsNoOp() {
        RecordingOpener opener = new RecordingOpener();
        ExplainMoreHandler handler = ExplainMoreHandler.delegatingTo(opener);
        handler.explain(null);
        assertEquals(0, opener.calls);
    }

    @Test
    void noopIsReplaceable() {
        ExplainMoreHandler noop = ExplainMoreHandler.delegatingTo(null);
        noop.explain(concept("Lambda"));
        noop.explain(null);
        assertTrue(true);
    }
}
