package com.eyecode.learning.swing;

import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardDocumentAdapter;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.RelatedConcept;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwingLearningCardTest {

    private SwingLearningCard realizeInFrame() {
        SwingLearningCard card = new SwingLearningCard();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(card);
        frame.pack();
        return card;
    }

    private LearningConcept conceptWith(String title, String description, List<String> related) {
        return conceptWith(title, description, com.eyecode.learning.model.ConceptType.CLASS, related);
    }

    private LearningConcept conceptWith(String title, String description,
                                       com.eyecode.learning.model.ConceptType type, List<String> related) {
        LearningConcept c = new LearningConcept();
        c.setTitle(title);
        c.setDescription(description);
        c.setType(type);
        c.setRelatedConcepts(related);
        return c;
    }

    @Test
    void renderNullDocumentClears() {
        SwingLearningCard card = realizeInFrame();
        card.render(null);
        assertEquals("", card.getHeader().title());
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
    }

    @Test
    void renderFillDocumentPopulates() {
        SwingLearningCard card = realizeInFrame();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addHeading("Class");
        doc.addParagraph("A class defines...");
        doc.addCodeBlock("Java", "int x;");
        doc.addBullet("Object");
        card.render(doc);
        assertEquals(1, card.getBody().getCodeBlocks().size());
    }

    @Test
    void bindActionsSetsActiveCodeSupplierToFirstCodeBlock() {
        SwingLearningCard card = realizeInFrame();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", "int x;");
        doc.addCodeBlock("Java", "int y;");
        card.render(doc);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        card.bindActions(rec, List.of());
        card.getActionBar().setActions(rec);
        assertEquals("int x;", card.getBody().getFirstCodeBlock().code());
    }

    @Test
    void bindActionsWithNoCodeBlockSetsNullSupplier() {
        SwingLearningCard card = realizeInFrame();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addParagraph("text only");
        card.render(doc);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        card.bindActions(rec, List.of());
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
    }

    @Test
    void clearResetsCard() {
        SwingLearningCard card = realizeInFrame();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addHeading("Class");
        doc.addCodeBlock("Java", "code");
        card.render(doc);
        card.clear();
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
    }

    @Test
    void contextSwapFromAtoBHasBCode() {
        SwingLearningCard card = realizeInFrame();

        LearningConcept a = conceptWith("Class", "A class defines",
                List.of("Inheritance"));
        LearningCardDocument docA = LearningCardDocumentAdapter.fromConcept(a);
        List<RelatedConcept> relatedA = LearningCardDocumentAdapter.relatedConceptsFrom(a);
        RecordingLearningCardActions recA = new RecordingLearningCardActions();
        card.render(docA);
        card.bindActions(recA, relatedA);
        String firstCodeA = card.getBody().getFirstCodeBlock().code();
        assertTrue(firstCodeA.contains("Class"));

        LearningConcept b = conceptWith("Lambda", "Anonymous function",
                List.of("Method Reference"));
        LearningCardDocument docB = LearningCardDocumentAdapter.fromConcept(b);
        List<RelatedConcept> relatedB = LearningCardDocumentAdapter.relatedConceptsFrom(b);
        RecordingLearningCardActions recB = new RecordingLearningCardActions();
        card.render(docB);
        card.bindActions(recB, relatedB);

        assertEquals("Lambda", docB.getHeader().title());
        String firstCodeB = card.getBody().getFirstCodeBlock().code();
        assertTrue(firstCodeB.contains("Lambda"));
        assertNotEquals(firstCodeA, firstCodeB);
        assertEquals(1, relatedB.size());
        assertEquals("Method Reference", relatedB.get(0).title());
    }

    @Test
    void contextSwapDoesNotKeepOldRelatedConcepts() {
        SwingLearningCard card = realizeInFrame();

        LearningConcept a = conceptWith("Class", "desc", List.of("Inheritance", "Polymorphism"));
        List<RelatedConcept> relatedA = LearningCardDocumentAdapter.relatedConceptsFrom(a);
        card.bindActions(new RecordingLearningCardActions(), relatedA);
        assertEquals(2, relatedA.size());

        LearningConcept b = conceptWith("Lambda", "desc", List.of());
        List<RelatedConcept> relatedB = LearningCardDocumentAdapter.relatedConceptsFrom(b);
        card.render(LearningCardDocumentAdapter.fromConcept(b));
        card.bindActions(new RecordingLearningCardActions(), relatedB);
        assertTrue(relatedB.isEmpty());
    }

    @Test
    void relatedConceptsEmptyInNewConceptDoesNotShowAConcepts() {
        SwingLearningCard card = realizeInFrame();
        LearningConcept a = conceptWith("Class", "desc", List.of("A", "B", "C"));
        LearningCardDocument docA = LearningCardDocumentAdapter.fromConcept(a);
        List<RelatedConcept> relatedA = LearningCardDocumentAdapter.relatedConceptsFrom(a);
        RecordingLearningCardActions recA = new RecordingLearningCardActions();
        card.render(docA);
        card.bindActions(recA, relatedA);
        assertEquals(3, relatedA.size());

        LearningConcept b = conceptWith("Lambda", "desc", List.of());
        LearningCardDocument docB = LearningCardDocumentAdapter.fromConcept(b);
        List<RelatedConcept> relatedB = LearningCardDocumentAdapter.relatedConceptsFrom(b);
        RecordingLearningCardActions recB = new RecordingLearningCardActions();
        card.render(docB);
        card.bindActions(recB, relatedB);
        assertTrue(relatedB.isEmpty());
    }

    @Test
    void lifecycleShowUpdateHideShowDisposeNoException() {
        SwingLearningCard card = realizeInFrame();
        LearningConcept a = conceptWith("Class", "desc", List.of("Inheritance"));
        LearningConcept b = conceptWith("Lambda", "desc", List.of("MethodRef"));

        card.render(LearningCardDocumentAdapter.fromConcept(a));
        card.bindActions(new RecordingLearningCardActions(),
                LearningCardDocumentAdapter.relatedConceptsFrom(a));
        card.render(LearningCardDocumentAdapter.fromConcept(b));
        card.bindActions(new RecordingLearningCardActions(),
                LearningCardDocumentAdapter.relatedConceptsFrom(b));
        card.clear();
        card.render(LearningCardDocumentAdapter.fromConcept(a));
        card.clear();
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
    }

    @Test
    void multipleCodeBlocksAllAccessible() {
        SwingLearningCard card = realizeInFrame();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", "first");
        doc.addCodeBlock("Java", "second");
        doc.addCodeBlock("Java", "third");
        card.render(doc);
        List<SwingCodeBlock> blocks = card.getBody().getCodeBlocks();
        assertEquals(3, blocks.size());
        assertEquals("first", blocks.get(0).code());
        assertEquals("second", blocks.get(1).code());
        assertEquals("third", blocks.get(2).code());
    }

    @Test
    void zeroCodeBlocksIsHandled() {
        SwingLearningCard card = realizeInFrame();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addParagraph("only text");
        card.render(doc);
        assertNull(card.getBody().getFirstCodeBlock());
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
    }
}
