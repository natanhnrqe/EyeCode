package com.eyecode.learning.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LearningCardDocumentTest {

    @Test
    void emptyDocumentHasNoBlocks() {
        LearningCardDocument doc = new LearningCardDocument();
        assertTrue(doc.getBlocks().isEmpty());
    }

    @Test
    void documentWithHeadingContainsBlock() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addHeading("Class");
        assertEquals(1, doc.getBlocks().size());
        assertTrue(doc.getBlocks().get(0) instanceof LearningCardBlock.HeadingBlock);
    }

    @Test
    void documentWithParagraphContainsBlock() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addParagraph("A class defines...");
        assertEquals(1, doc.getBlocks().size());
        assertInstanceOf(LearningCardBlock.ParagraphBlock.class, doc.getBlocks().get(0));
    }

    @Test
    void documentWithCodeBlockContainsBlock() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addCodeBlock("Java", "public class A {}");
        assertEquals(1, doc.getBlocks().size());
        assertInstanceOf(LearningCardBlock.CodeBlock.class, doc.getBlocks().get(0));
    }

    @Test
    void documentWithBulletContainsBlock() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addBullet("Object");
        assertEquals(1, doc.getBlocks().size());
        assertInstanceOf(LearningCardBlock.BulletBlock.class, doc.getBlocks().get(0));
    }

    @Test
    void documentPreservesOrder() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addHeading("Class");
        doc.addParagraph("Text");
        doc.addCodeBlock("Java", "{}");
        doc.addBullet("Object");

        List<LearningCardBlock> blocks = doc.getBlocks();
        assertEquals(4, blocks.size());
        assertInstanceOf(LearningCardBlock.HeadingBlock.class, blocks.get(0));
        assertInstanceOf(LearningCardBlock.ParagraphBlock.class, blocks.get(1));
        assertInstanceOf(LearningCardBlock.CodeBlock.class, blocks.get(2));
        assertInstanceOf(LearningCardBlock.BulletBlock.class, blocks.get(3));
    }

    @Test
    void clearRemovesAllBlocks() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addHeading("X");
        doc.clear();
        assertTrue(doc.getBlocks().isEmpty());
    }

    @Test
    void headerAndFooterCanBeSet() {
        LearningCardDocument doc = new LearningCardDocument(
                new LearningCardHeaderData("Class", "Inheritance"),
                new LearningCardFooterData("Updated:", "Today")
        );
        assertNotNull(doc.getHeader());
        assertNotNull(doc.getFooter());
    }
}
