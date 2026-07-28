package com.eyecode.learning.swing;

import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardDocumentAdapter;
import com.eyecode.learning.model.LearningConcept;
import org.junit.jupiter.api.Test;

import javax.swing.AbstractButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import java.awt.Container;
import java.awt.Dimension;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwingLearningCardVisualStyleTest {

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

    private AbstractButton findButton(SwingLearningActionBar bar, String text) {
        for (java.awt.Component comp : bar.getComponents()) {
            if (comp instanceof AbstractButton b && text.equals(b.getText())) {
                return b;
            }
        }
        throw new AssertionError("Button not found: " + text);
    }

    @Test
    void headerMaintainsTitleAndSubtitleText() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Class", "desc", ConceptType.CLASS, List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        assertEquals("Class", card.getHeader().title());
        assertTrue(card.getHeader().subtitle().contains("CLASS")
                || card.getHeader().subtitle().contains("BEGINNER"));
    }

    @Test
    void footerIsInvisibleWhenNotPresentInDocument() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addHeading("Class");
        card.render(doc);
        assertFalse(card.getFooter().isVisible());
    }

    @Test
    void footerIsVisibleWhenPresentInDocument() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Class", "desc", ConceptType.CLASS, List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        assertTrue(card.getFooter().isVisible());
    }

    @Test
    void codeBlockHasNoInternalScrollPaneForLongCode() {
        SwingLearningCard card = realize();
        StringBuilder sb = new StringBuilder();
        sb.append("public class Big {\n");
        for (int i = 0; i < 100; i++) {
            sb.append("    public void m").append(i).append("() {}\n");
        }
        sb.append("}\n");
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", sb.toString());
        card.render(doc);
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        assertNotNull(block);
        assertFalse(containsJScrollPane(block),
                "SwingCodeBlock must not contain an internal JScrollPane, got one for long code");
        assertTrue(block.code().length() > 500,
                "Long code should be preserved");
    }

    @Test
    void copyCodeButtonDisabledWhenNoCode() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Class", "x", ConceptType.CLASS, List.of());
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(c);
        doc.clear();
        doc.addHeading("Class");
        doc.addParagraph("only text");
        card.render(doc);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        card.bindActions(rec, List.of());
        AbstractButton copy = findButton(card.getActionBar(),
                SwingLearningActionBar.ACTION_COPY_CODE);
        assertFalse(copy.isEnabled());
    }

    @Test
    void relatedButtonDisabledWhenEmpty() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Class", "x", ConceptType.CLASS, List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        card.bindActions(rec, List.of());
        AbstractButton related = findButton(card.getActionBar(),
                SwingLearningActionBar.ACTION_RELATED);
        assertFalse(related.isEnabled());
    }

    @Test
    void relatedButtonEnabledWhenAtLeastOneRelated() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Class", "x", ConceptType.CLASS, List.of("object"));
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        card.bindActions(rec,
                com.eyecode.learning.model.LearningCardDocumentAdapter.relatedConceptsFrom(c));
        AbstractButton related = findButton(card.getActionBar(),
                SwingLearningActionBar.ACTION_RELATED);
        assertTrue(related.isEnabled());
    }

    @Test
    void multipleCodeBlocksRemainAccessible() {
        SwingLearningCard card = realize();
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
    void contextSwapAToBLeavesNoArtifact() {
        SwingLearningCard card = realize();
        LearningConcept a = concept("Class", "AAA description", ConceptType.CLASS,
                List.of("object"));
        card.render(LearningCardDocumentAdapter.fromConcept(a));
        card.bindActions(new RecordingLearningCardActions(),
                LearningCardDocumentAdapter.relatedConceptsFrom(a));

        LearningConcept b = concept("Lambda", "BBB description", ConceptType.RECORD,
                List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(b));
        card.bindActions(new RecordingLearningCardActions(),
                LearningCardDocumentAdapter.relatedConceptsFrom(b));

        assertEquals("Lambda", card.getHeader().title());
        assertTrue(card.getHeader().subtitle().contains("RECORD"));
        AbstractButton related = findButton(card.getActionBar(),
                SwingLearningActionBar.ACTION_RELATED);
        assertFalse(related.isEnabled());

        AbstractButton copy = findButton(card.getActionBar(),
                SwingLearningActionBar.ACTION_COPY_CODE);
        assertTrue(copy.isEnabled());
    }

    @Test
    void cardHasBoundedMaximumSize() {
        SwingLearningCard card = realize();
        Dimension max = card.getMaximumSize();
        assertNotNull(max);
        assertTrue(max.width <= SwingLearningCardStyle.CARD_MAX_WIDTH);
        assertTrue(max.height <= SwingLearningCardStyle.CARD_MAX_HEIGHT);
    }

    @Test
    void headerIconClearedOnNullDocument() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Class", "x", ConceptType.CLASS,
                List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        assertNotNull(card.getHeader().title());
        card.render(null);
        assertEquals("", card.getHeader().title());
        assertEquals("", card.getHeader().subtitle());
        assertFalse(card.getFooter().isVisible());
    }

    @Test
    void disabledButtonsHaveDisabledForeground() {
        SwingLearningCard card = realize();
        card.render(new LearningCardDocument());
        card.bindActions(new RecordingLearningCardActions(), List.of());
        AbstractButton copy = findButton(card.getActionBar(),
                SwingLearningActionBar.ACTION_COPY_CODE);
        AbstractButton related = findButton(card.getActionBar(),
                SwingLearningActionBar.ACTION_RELATED);
        assertFalse(copy.isEnabled());
        assertFalse(related.isEnabled());
        assertEquals(com.eyecode.ui.designsystem.ColorManager.TEXT_DISABLED,
                copy.getForeground());
        assertEquals(com.eyecode.ui.designsystem.ColorManager.TEXT_DISABLED,
                related.getForeground());
    }

    @Test
    void smallConceptProducesCompactCard() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Enum", "a tiny concept", ConceptType.ENUM, List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        assertEquals("Enum", card.getHeader().title());
        assertTrue(card.getBody().getCodeBlocks().size() == 1);
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        assertNotNull(block);
        assertFalse(containsJScrollPane(block),
                "SwingCodeBlock must not contain an internal JScrollPane");
    }

    private boolean containsJScrollPane(Container c) {
        for (java.awt.Component comp : c.getComponents()) {
            if (comp instanceof JScrollPane) {
                return true;
            }
            if (comp instanceof Container sub && containsJScrollPane(sub)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void clearResetsAllVisualElements() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Class", "desc", ConceptType.CLASS, List.of("object"));
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        card.bindActions(new RecordingLearningCardActions(),
                LearningCardDocumentAdapter.relatedConceptsFrom(c));
        card.clear();
        assertEquals("", card.getHeader().title());
        assertEquals("", card.getHeader().subtitle());
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
        assertFalse(card.getFooter().isVisible());
    }
}
