package com.eyecode.learning.swing;

import com.eyecode.learning.model.LearningCardDocument;
import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import java.awt.Container;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SwingCodeBlockMultipleTest {

    private SwingLearningCard realize() {
        SwingLearningCard card = new SwingLearningCard();
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.add(card);
        frame.pack();
        return card;
    }

    @Test
    void multipleCodeBlocksEachHasOwnCopy() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", "code A");
        doc.addCodeBlock("Java", "code B");
        doc.addCodeBlock("Java", "code C");
        card.render(doc);

        List<SwingCodeBlock> blocks = card.getBody().getCodeBlocks();
        assertEquals(3, blocks.size());
        assertEquals("code A", blocks.get(0).code());
        assertEquals("code B", blocks.get(1).code());
        assertEquals("code C", blocks.get(2).code());
    }

    @Test
    void globalCopyConcatenatesAllBlocks() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", "code A");
        doc.addCodeBlock("Java", "code B");
        doc.addCodeBlock("Java", "code C");
        card.render(doc);

        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        card.bindActions(rec, doc.getRelatedConcepts());

        javax.swing.AbstractButton copyButton = findButtonBy(card.getActionBar(),
                SwingLearningActionBar.ACTION_COPY_CODE);
        copyButton.doClick();

        assertEquals(1, rec.copiedCodes().size());
        String copied = rec.lastCopiedCode();
        assertTrue(copied.contains("code A"));
        assertTrue(copied.contains("code B"));
        assertTrue(copied.contains("code C"));
    }

    @Test
    void singleCodeBlockCopiedCopyFromIndividualButtonStillWorks() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", "only code");
        card.render(doc);
        SwingCodeBlock first = card.getBody().getFirstCodeBlock();
        assertNotNull(first);
        assertEquals("only code", first.code());
        assertDoesNotThrow(first::copy);
    }

    @Test
    void emptyCodeBuilderResultsInNoCodeBlock() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", "");
        doc.addCodeBlock("Java", null);
        card.render(doc);
        assertTrue(card.getBody().getCodeBlocks().isEmpty());
    }

    @Test
    void longCodeDoesNotExpandCardWidthAndHasNoInternalScroll() {
        SwingLearningCard card = realize();
        StringBuilder longCode = new StringBuilder();
        longCode.append("public class Big {\n");
        for (int i = 0; i < 50; i++) {
            longCode.append("    public void method").append(i).append("() { /* lots of code */ }\n");
        }
        longCode.append("}\n");

        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", longCode.toString());
        card.render(doc);

        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        assertNotNull(block);
        assertFalse(containsJScrollPane(block),
                "SwingCodeBlock must not contain an internal JScrollPane");
        assertTrue(block.code().length() > 500,
                "Long code should be preserved");

        int cardWidth = card.getPreferredSize().width;
        assertTrue(cardWidth <= SwingLearningCardStyle.CARD_MAX_WIDTH,
                "Card width should remain bounded, got " + cardWidth);
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
    void longCodePreservesWhitespace() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        String code = "public class A {\n" +
                "    public void method() {\n" +
                "        if (true) {\n" +
                "            System.out.println(\"indented\");\n" +
                "        }\n" +
                "    }\n" +
                "}\n";
        doc.addCodeBlock("Java", code);
        card.render(doc);
        SwingCodeBlock block = card.getBody().getFirstCodeBlock();
        assertEquals(code, block.code());
    }

    @Test
    void copyButtonDisabledWhenNoCode() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addParagraph("only text");
        card.render(doc);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        card.bindActions(rec, doc.getRelatedConcepts());
        javax.swing.AbstractButton copyButton = findButtonBy(card.getActionBar(),
                SwingLearningActionBar.ACTION_COPY_CODE);
        assertFalse(copyButton.isEnabled());
    }

    @Test
    void copyButtonEnabledWhenCodeAtLeastOneBlock() {
        SwingLearningCard card = realize();
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", "int x = 1;");
        card.render(doc);
        RecordingLearningCardActions rec = new RecordingLearningCardActions();
        card.bindActions(rec, doc.getRelatedConcepts());
        javax.swing.AbstractButton copyButton = findButtonBy(card.getActionBar(),
                SwingLearningActionBar.ACTION_COPY_CODE);
        assertTrue(copyButton.isEnabled());
    }

    private javax.swing.AbstractButton findButtonBy(SwingLearningActionBar bar, String text) {
        return findButtons(bar).stream()
                .filter(b -> text.equals(b.getText()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Button not found: " + text));
    }

    private java.util.List<javax.swing.AbstractButton> findButtons(java.awt.Container c) {
        java.util.List<javax.swing.AbstractButton> out = new java.util.ArrayList<>();
        for (java.awt.Component comp : c.getComponents()) {
            if (comp instanceof javax.swing.AbstractButton b) {
                out.add(b);
            }
            if (comp instanceof java.awt.Container cont) {
                out.addAll(findButtons(cont));
            }
        }
        return out;
    }
}
