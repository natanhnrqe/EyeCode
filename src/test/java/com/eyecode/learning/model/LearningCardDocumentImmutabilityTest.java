package com.eyecode.learning.model;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LearningCardDocumentImmutabilityTest {

    @Test
    void getBlocksReturnsUnmodifiableView() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addHeading("Class");
        List<LearningCardBlock> blocks = doc.getBlocks();
        assertThrows(UnsupportedOperationException.class, () -> blocks.add(new LearningCardBlock.HeadingBlock("X")));
    }

    @Test
    void getCodeBlocksReturnsOnlyCodeBlocks() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addHeading("Java");
        doc.addCodeBlock("Java", "public class A {}");
        doc.addParagraph("text");
        doc.addCodeBlock("Java", "int x;");
        List<LearningCardBlock.CodeBlock> codeBlocks = doc.getCodeBlocks();
        assertEquals(2, codeBlocks.size());
        assertEquals("public class A {}", codeBlocks.get(0).code());
    }

    @Test
    void getBulletsReturnsOnlyBulletBlocks() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addBullet("a");
        doc.addBullet("b");
        doc.addHeading("Other");
        List<LearningCardBlock.BulletBlock> bullets = doc.getBullets();
        assertEquals(2, bullets.size());
    }

    @Test
    void clearResetsBlocks() {
        LearningCardDocument doc = new LearningCardDocument();
        doc.addHeading("A");
        doc.addCodeBlock("Java", "{}");
        doc.clear();
        assertTrue(doc.getBlocks().isEmpty());
        assertTrue(doc.getCodeBlocks().isEmpty());
    }
}
