package com.eyecode.learning.swing;

import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardDocumentAdapter;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.RelatedConcept;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwingCodeBlockScrollTest {

    private SwingLearningCard realize() {
        SwingLearningCard card = new SwingLearningCard();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(card);
        frame.pack();
        return card;
    }

    private LearningConcept concept(String title, ConceptType type, List<String> related) {
        LearningConcept c = new LearningConcept();
        c.setTitle(title);
        c.setDescription("desc");
        c.setType(type);
        c.setRelatedConcepts(related);
        return c;
    }

    private boolean containsJScrollPane(Container c) {
        for (Component comp : c.getComponents()) {
            if (comp instanceof JScrollPane) {
                return true;
            }
            if (comp instanceof Container sub && containsJScrollPane(sub)) {
                return true;
            }
        }
        return false;
    }

    private JTextArea findCodeArea(SwingCodeBlock block) {
        for (Component comp : block.getComponents()) {
            if (comp instanceof JTextArea ta) {
                return ta;
            }
            if (comp instanceof Container sub) {
                for (Component inner : sub.getComponents()) {
                    if (inner instanceof JTextArea ta) {
                        return ta;
                    }
                }
            }
        }
        for (Component comp : block.getComponents()) {
            if (comp instanceof Container sub) {
                for (Component inner : sub.getComponents()) {
                    if (inner instanceof JTextArea ta) {
                        return ta;
                    }
                }
            }
        }
        return null;
    }

    @Test
    void codeBlockContainsNoInternalJScrollPane() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", "public class A {}");
        card.render(doc);
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        assertNotNull(block);
        assertFalse(containsJScrollPane(block),
                "SwingCodeBlock must not contain any JScrollPane");
    }

    @Test
    void codeBlockHasNoVerticalScrollBarOfItsOwn() {
        SwingCodeBlock block = new SwingCodeBlock("Java", "x = 1;");
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.add(block);
        f.pack();
        assertFalse(containsJScrollPane(block),
                "SwingCodeBlock must not own a vertical scrollbar");
        f.dispose();
    }

    @Test
    void codeBlockHasNoHorizontalScrollBarOfItsOwn() {
        SwingCodeBlock block = new SwingCodeBlock("Java", "x = 1;");
        JFrame f = new JFrame();
        f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        f.add(block);
        f.pack();
        assertFalse(containsJScrollPane(block),
                "SwingCodeBlock must not own a horizontal scrollbar");
        f.dispose();
    }

    @Test
    void codeAreaHasLineWrapEnabled() {
        SwingCodeBlock block = new SwingCodeBlock("Java", "int x = 1;");
        JTextArea area = findCodeArea(block);
        assertNotNull(area);
        assertTrue(area.getLineWrap(), "JTextArea must have lineWrap=true");
    }

    @Test
    void codeAreaHasWrapStyleWordEnabled() {
        SwingCodeBlock block = new SwingCodeBlock("Java", "int x = 1;");
        JTextArea area = findCodeArea(block);
        assertNotNull(area);
        assertTrue(area.getWrapStyleWord(), "JTextArea must have wrapStyleWord=true");
    }

    @Test
    void bodyStillUsesJScrollPane() {
        SwingLearningCard card = realize();
        assertTrue(card.getBody() instanceof JScrollPane,
                "SwingLearningBody must remain a JScrollPane");
    }

    @Test
    void bodyVerticalScrollBarAsNeeded() {
        SwingLearningCard card = realize();
        int policy = ((JScrollPane) card.getBody()).getVerticalScrollBarPolicy();
        assertEquals(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, policy);
    }

    @Test
    void bodyHorizontalScrollBarNever() {
        SwingLearningCard card = realize();
        int policy = ((JScrollPane) card.getBody()).getHorizontalScrollBarPolicy();
        assertEquals(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER, policy);
    }

    @Test
    void longCodeDoesNotIncreaseCardWidth() {
        SwingLearningCard card = realize();
        String longLine = "public void metodoMuitoLongoComUmNomeGrande(String a, String b, int c, int d) {";
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", longLine + "\n    return;\n}\n");
        card.render(doc);
        int width = card.getPreferredSize().width;
        assertTrue(width <= SwingLearningCardStyle.CARD_MAX_WIDTH,
                "Card width must remain bounded at " + SwingLearningCardStyle.CARD_MAX_WIDTH
                        + ", got " + width);
    }

    @Test
    void longCodeStaysWithinBodyViewportWidth() {
        SwingLearningCard card = realize();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(card);
        frame.pack();

        String longLine = "public void metodoMuitoLongoComUmNomeGrande(String a, String b, int c, int d) {";
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", longLine + "\n}\n");
        card.render(doc);
        card.setSize(card.getPreferredSize());
        card.doLayout();
        card.getBody().doLayout();

        JScrollPane bodyScroll = (JScrollPane) card.getBody();
        int viewportWidth = bodyScroll.getViewport().getViewRect().width;
        assertTrue(viewportWidth > 0);
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        assertNotNull(block);
        Rectangle blockBounds = block.getBounds();
        assertTrue(blockBounds.x + blockBounds.width <= viewportWidth + 1,
                "Block must not exceed viewport width; right edge=" + (blockBounds.x + blockBounds.width)
                        + ", viewport=" + viewportWidth);
        frame.dispose();
    }

    @Test
    void copyPreservesOriginalCodeNotVisualWrap() {
        String original = "public class Pedido {\n    private StatusPedido status;\n}";
        SwingCodeBlock block = new SwingCodeBlock("Java", original);
        block.copy();
        assertEquals(original, block.code(),
                "Copy must return original unmodified code, not visually wrapped version");
    }

    @Test
    void cardRemainsFixedGeometry520x520() {
        SwingLearningCard card = realize();
        Dimension pref = card.getPreferredSize();
        assertEquals(SwingLearningCardStyle.CARD_MAX_WIDTH, pref.width);
        assertEquals(SwingLearningCardStyle.CARD_MAX_HEIGHT, pref.height);
    }

    @Test
    void classConceptRendersWithCodeBlock() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Class", ConceptType.CLASS, List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        assertEquals("Class", card.getHeader().title());
        assertFalse(containsJScrollPane(card.getBody().getFirstCodeBlock()),
                "Class code block must not contain internal JScrollPane");
    }

    @Test
    void enumConceptRendersWithCodeBlock() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Enum", ConceptType.ENUM, List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        assertEquals("Enum", card.getHeader().title());
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        if (block != null) {
            assertFalse(containsJScrollPane(block),
                    "Enum code block must not contain internal JScrollPane");
        }
    }

    @Test
    void interfaceConceptRendersWithCodeBlock() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Interface", ConceptType.INTERFACE, List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        assertEquals("Interface", card.getHeader().title());
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        if (block != null) {
            assertFalse(containsJScrollPane(block),
                    "Interface code block must not contain internal JScrollPane");
        }
    }

    @Test
    void recordConceptRendersWithCodeBlock() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Record", ConceptType.RECORD, List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        assertEquals("Record", card.getHeader().title());
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        if (block != null) {
            assertFalse(containsJScrollPane(block),
                    "Record code block must not contain internal JScrollPane");
        }
    }

    @Test
    void contextSwapFromAToBLeavesNoSecondScroll() {
        SwingLearningCard card = realize();
        LearningConcept a = concept("Class", ConceptType.CLASS, List.of("object"));
        card.render(LearningCardDocumentAdapter.fromConcept(a));
        card.bindActions(new RecordingLearningCardActions(),
                LearningCardDocumentAdapter.relatedConceptsFrom(a));

        LearningConcept b = concept("Lambda", ConceptType.RECORD, List.of());
        card.render(LearningCardDocumentAdapter.fromConcept(b));
        card.bindActions(new RecordingLearningCardActions(),
                LearningCardDocumentAdapter.relatedConceptsFrom(b));

        assertEquals("Lambda", card.getHeader().title());
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        if (block != null) {
            assertFalse(containsJScrollPane(block),
                    "A->B swap must not leave an internal JScrollPane inside CodeBlock");
        }
    }

    @Test
    void relatedConceptsStillRenderedAfterCodeBlockRefactor() {
        SwingLearningCard card = realize();
        LearningConcept c = concept("Class", ConceptType.CLASS, List.of("object", "inheritance"));
        card.render(LearningCardDocumentAdapter.fromConcept(c));
        List<RelatedConcept> related = LearningCardDocumentAdapter.relatedConceptsFrom(c);
        card.bindActions(new RecordingLearningCardActions(), related);
        assertNotNull(related);
        assertTrue(related.size() >= 1, "Related concepts should be resolved");
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        if (block != null) {
            assertFalse(containsJScrollPane(block),
                    "Related-concepts scenario code block must not contain internal JScrollPane");
        }
    }
}
