package com.eyecode.learning.swing;

import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.LearningCardDocumentAdapter;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwingLearningCardContextReplacementTest {

    private SwingLearningCard realize() {
        SwingLearningCard card = new SwingLearningCard();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(card);
        frame.pack();
        return card;
    }

    private LearningConcept concept(String title, String description, ConceptType type,
                                    List<String> related) {
        LearningConcept c = new LearningConcept();
        c.setTitle(title);
        c.setDescription(description);
        c.setType(type);
        c.setRelatedConcepts(related);
        return c;
    }

    @Test
    void titleBReplacesTitleA() {
        SwingLearningCard card = realize();
        LearningCardDocument docA = LearningCardDocumentAdapter.fromConcept(
                concept("Class", "desc A", ConceptType.CLASS, List.of()));
        card.render(docA);
        assertEquals("Class", card.getHeader().title());

        LearningCardDocument docB = LearningCardDocumentAdapter.fromConcept(
                concept("Lambda", "desc B", ConceptType.RECORD, List.of()));
        card.render(docB);
        assertEquals("Lambda", card.getHeader().title());
        assertNotEquals("Class", card.getHeader().title());
    }

    @Test
    void subtitleBReplacesSubtitleA() {
        SwingLearningCard card = realize();
        LearningCardDocument docA = LearningCardDocumentAdapter.fromConcept(
                concept("Class", "desc", ConceptType.CLASS, List.of()));
        card.render(docA);
        assertTrue(card.getHeader().subtitle().contains("CLASS"));

        LearningCardDocument docB = LearningCardDocumentAdapter.fromConcept(
                concept("Lambda", "desc", ConceptType.RECORD, List.of()));
        card.render(docB);
        assertTrue(card.getHeader().subtitle().contains("RECORD"));
    }

    @Test
    void descriptionBReplacesDescriptionA() {
        SwingLearningCard card = realize();
        LearningCardDocument docA = LearningCardDocumentAdapter.fromConcept(
                concept("Class", "A class blueprint", ConceptType.CLASS, List.of()));
        card.render(docA);
        long paragraphA = docA.getBlocks().stream()
                .filter(b -> b instanceof com.eyecode.learning.model.LearningCardBlock.ParagraphBlock p
                        && p.text().equals("A class blueprint"))
                .count();
        assertEquals(1, paragraphA);

        LearningCardDocument docB = LearningCardDocumentAdapter.fromConcept(
                concept("Lambda", "Anonymous fn", ConceptType.RECORD, List.of()));
        card.render(docB);
        long paragraphB = docB.getBlocks().stream()
                .filter(b -> b instanceof com.eyecode.learning.model.LearningCardBlock.ParagraphBlock p
                        && p.text().equals("Anonymous fn"))
                .count();
        assertEquals(1, paragraphB);
    }

    @Test
    void codeBReplacesCodeA() {
        SwingLearningCard card = realize();
        LearningCardDocument docA = LearningCardDocumentAdapter.fromConcept(
                concept("Class", "desc", ConceptType.CLASS, List.of()));
        card.render(docA);
        String codeA = card.getBody().getFirstCodeBlock().code();
        assertTrue(codeA.contains("class Class"));

        LearningCardDocument docB = LearningCardDocumentAdapter.fromConcept(
                concept("Lambda", "fn", ConceptType.RECORD, List.of()));
        card.render(docB);
        String codeB = card.getBody().getFirstCodeBlock().code();
        assertTrue(codeB.contains("class Lambda"));
        assertNotEquals(codeA, codeB);
    }

    @Test
    void relatedConceptsBReplacesRelatedConceptsA() {
        SwingLearningCard card = realize();
        LearningConcept a = concept("Class", "desc", ConceptType.CLASS, List.of("object", "interface"));
        LearningCardDocument docA = LearningCardDocumentAdapter.fromConcept(a);
        card.render(docA);
        assertEquals(2, docA.getRelatedConcepts().size());

        LearningConcept b = concept("Lambda", "desc", ConceptType.RECORD, List.of());
        LearningCardDocument docB = LearningCardDocumentAdapter.fromConcept(b);
        card.render(docB);
        assertTrue(docB.getRelatedConcepts().isEmpty());
    }

    @Test
    void footerBReplacesFooterA() {
        SwingLearningCard card = realize();
        LearningCardDocument docA = LearningCardDocumentAdapter.fromConcept(
                concept("Class", "desc", ConceptType.CLASS, List.of()));
        card.render(docA);
        assertTrue(card.getFooter().isVisible());

        LearningCardDocument docB = new LearningCardDocument();
        card.render(docB);
        assertFalse(card.getFooter().isVisible());
    }

    @Test
    void fullContextSwapLeavesNoArtifact() {
        SwingLearningCard card = realize();
        LearningConcept a = concept("Class", "AAA", ConceptType.CLASS, List.of("object"));
        LearningCardDocument docA = LearningCardDocumentAdapter.fromConcept(a);
        card.render(docA);
        RecordingLearningCardActions recA = new RecordingLearningCardActions();
        card.bindActions(recA, docA.getRelatedConcepts());

        LearningConcept b = concept("Lambda", "BBB", ConceptType.RECORD, List.of("object", "class"));
        LearningCardDocument docB = LearningCardDocumentAdapter.fromConcept(b);
        card.render(docB);
        RecordingLearningCardActions recB = new RecordingLearningCardActions();
        card.bindActions(recB, docB.getRelatedConcepts());

        assertEquals("Lambda", card.getHeader().title());
        assertTrue(card.getHeader().subtitle().contains("RECORD"));
        assertEquals(2, docB.getRelatedConcepts().size());

        long aaaParagraphs = card.getBody().getContentPanel().getComponentCount() == 0 ? 0 :
                java.util.Arrays.stream(card.getBody().getContentPanel().getComponents())
                        .filter(c -> c instanceof SwingLearningParagraph p && p.getText().contains("AAA"))
                        .count();
        assertEquals(0, aaaParagraphs);
    }
}
