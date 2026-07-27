package com.eyecode.learning.swing;

import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardDocumentAdapter;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.ConceptType;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwingLearningCardEmptyStatesTest {

    private SwingLearningCard realize() {
        SwingLearningCard card = new SwingLearningCard();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(card);
        frame.pack();
        return card;
    }

    private LearningConcept concept() {
        LearningConcept c = new LearningConcept();
        return c;
    }

    @Test
    void emptyConceptClearsCard() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(new LearningConcept());
        card.render(doc);
        assertEquals("", card.getHeader().title());
        assertEquals("", card.getHeader().subtitle());
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
        assertEquals(0, card.getBody().getAllCodeJoined().length());
    }

    @Test
    void conceptWithoutDescriptionHasNoParagraph() {
        SwingLearningCard card = realize();
        LearningConcept c = new LearningConcept();
        c.setTitle("Class");
        c.setType(ConceptType.CLASS);
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(c);
        card.render(doc);
        long paragraphCount = doc.getBlocks().stream()
                .filter(b -> b instanceof com.eyecode.learning.model.LearningCardBlock.ParagraphBlock)
                .count();
        assertEquals(0, paragraphCount);
    }

    @Test
    void conceptWithoutTypeHasNoCodeBlock() {
        SwingLearningCard card = realize();
        LearningConcept c = new LearningConcept();
        c.setTitle("Lambda");
        c.setDescription("Anonymous function");
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(c);
        card.render(doc);
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
        assertFalse(card.getBody().hasCode());
    }

    @Test
    void conceptWithoutRelatedConceptsHasEmptyRelated() {
        SwingLearningCard card = realize();
        LearningConcept c = new LearningConcept();
        c.setTitle("Class");
        c.setType(ConceptType.CLASS);
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(c);
        card.render(doc);
        assertTrue(doc.getRelatedConcepts().isEmpty());
    }

    @Test
    void conceptWithRelatedConceptsPopulatesDocument() {
        SwingLearningCard card = realize();
        LearningConcept c = new LearningConcept();
        c.setTitle("Class");
        c.setType(ConceptType.CLASS);
        c.setRelatedConcepts(List.of("object", "interface"));
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(c);
        card.render(doc);
        assertEquals(2, doc.getRelatedConcepts().size());
    }

    @Test
    void conceptWithoutTitleRendersEmptyHeader() {
        SwingLearningCard card = realize();
        LearningConcept c = new LearningConcept();
        c.setDescription("just description");
        c.setType(ConceptType.CLASS);
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(c);
        card.render(doc);
        assertEquals("", card.getHeader().title());
    }

    @Test
    void partiallyFilledConceptIsStillValid() {
        SwingLearningCard card = realize();
        LearningConcept c = new LearningConcept();
        c.setTitle("Partial");
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(c);
        card.render(doc);
        assertEquals("Partial", card.getHeader().title());
        assertTrue(doc.getRelatedConcepts().isEmpty());
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
    }

    @Test
    void clearDoesNotThrowAndResets() {
        SwingLearningCard card = realize();
        assertDoesNotThrow(card::clear);
        assertEquals("", card.getHeader().title());
        assertFalse(card.getFooter().isVisible());
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
    }

    @Test
    void emptyDocumentHasVisibleFooter() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(new LearningConcept());
        card.render(doc);
        assertTrue(card.getFooter().isVisible(),
                "Footer should be visible when present (even if empty text)");
    }

    @Test
    void nullConceptDocumentHasVisibleFooter() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        doc.setFooter(new com.eyecode.learning.model.LearningCardFooterData("Updated:", "Today"));
        card.render(doc);
        assertTrue(card.getFooter().isVisible());
    }

    @Test
    void documentWithoutFooterHidesFooter() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        card.render(doc);
        assertFalse(card.getFooter().isVisible());
    }
}
