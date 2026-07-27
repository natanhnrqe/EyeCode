package com.eyecode.learning.swing;

import com.eyecode.learning.model.RelatedConcept;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import java.awt.Component;
import java.awt.Container;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwingLearningActionBarTest {

    private static List<AbstractButton> findButtons(Container c) {
        List<AbstractButton> out = new ArrayList<>();
        for (Component comp : c.getComponents()) {
            if (comp instanceof AbstractButton b) {
                out.add(b);
            }
            if (comp instanceof Container cont) {
                out.addAll(findButtons(cont));
            }
        }
        return out;
    }

    private static AbstractButton findButton(SwingLearningActionBar bar, String text) {
        return findButtons(bar).stream()
                .filter(b -> text.equals(b.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    private void realizeInFrame(SwingLearningActionBar bar) {
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(bar);
        frame.pack();
    }

    @Test
    void openDocsButtonTriggersOpenDocumentation() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        bar.setActions(rec);
        findButton(bar, "Open Documentation").doClick();
        assertEquals(1, rec.openDocCalls());
    }

    @Test
    void explainButtonTriggersExplainMore() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        bar.setActions(rec);
        findButton(bar, "Explain More").doClick();
        assertEquals(1, rec.explainCalls());
    }

    @Test
    void copyButtonUsesActiveCodeSupplier() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        bar.setActions(rec);
        bar.setActiveCodeSupplier(() -> "int x = 42;");
        findButton(bar, "Copy Code").doClick();
        assertEquals(List.of("int x = 42;"), rec.copiedCodes());
    }

    @Test
    void copyButtonWithoutActiveSupplierSendsNoCode() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        bar.setActions(rec);
        bar.setActiveCodeSupplier(() -> null);
        findButton(bar, "Copy Code").doClick();
        assertTrue(rec.copiedCodes().isEmpty());
    }

    @Test
    void copyButtonWithEmptyCodeSendsNoCode() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        bar.setActions(rec);
        bar.setActiveCodeSupplier(() -> "");
        findButton(bar, "Copy Code").doClick();
        assertTrue(rec.copiedCodes().isEmpty());
    }

    @Test
    void relatedButtonTriggersShowRelatedConceptsWithStoredConcepts() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        bar.setActions(rec);
        List<RelatedConcept> related = List.of(
                RelatedConcept.of("inh", "Inheritance"),
                RelatedConcept.of("poly", "Polymorphism")
        );
        bar.setRelatedConcepts(related);
        findButton(bar, "Related Concepts").doClick();
        assertEquals(1, rec.relatedShown().size());
        assertEquals(related, rec.lastRelatedShown());
    }

    @Test
    void relatedButtonDisabledWhenEmptyConcepts() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        bar.setRelatedConcepts(List.of());
        assertFalse(findButton(bar, "Related Concepts").isEnabled());
    }

    @Test
    void relatedButtonEnabledWhenConceptsProvided() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        bar.setRelatedConcepts(List.of(RelatedConcept.of("x", "X")));
        assertTrue(findButton(bar, "Related Concepts").isEnabled());
    }

    @Test
    void copyButtonDisabledWhenSupplierReturnsNull() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        bar.setActiveCodeSupplier(() -> null);
        assertFalse(findButton(bar, "Copy Code").isEnabled());
    }

    @Test
    void copyButtonDisabledWhenSupplierReturnsEmptyString() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        bar.setActiveCodeSupplier(() -> "");
        assertFalse(findButton(bar, "Copy Code").isEnabled());
    }

    @Test
    void copyButtonEnabledWhenSupplierReturnsCode() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        bar.setActiveCodeSupplier(() -> "int x;");
        assertTrue(findButton(bar, "Copy Code").isEnabled());
    }

    @Test
    void clearActionsResetsAndDisables() {
        SwingLearningActionBar bar = new SwingLearningActionBar();
        realizeInFrame(bar);
        bar.setActiveCodeSupplier(() -> "x");
        bar.setRelatedConcepts(List.of(RelatedConcept.of("a", "A")));
        bar.clearActions();
        assertFalse(findButton(bar, "Copy Code").isEnabled());
        assertFalse(findButton(bar, "Related Concepts").isEnabled());
        assertFalse(findButton(bar, "Explain More").isEnabled());
    }

    @Test
    void noopActionsAreSafe() {
        LearningCardActions noop = LearningCardActions.noop();
        assertDoesNotThrow(() -> noop.openDocumentation());
        assertDoesNotThrow(() -> noop.explainMore());
        assertDoesNotThrow(() -> noop.copyCode("code"));
        assertDoesNotThrow(() -> noop.showRelatedConcepts(List.of()));
    }
}
