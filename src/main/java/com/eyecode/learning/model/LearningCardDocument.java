package com.eyecode.learning.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class LearningCardDocument {

    private LearningCardHeaderData header;
    private LearningCardFooterData footer;
    private final List<LearningCardBlock> blocks;

    public LearningCardDocument() {
        this.blocks = new ArrayList<>();
    }

    public LearningCardDocument(LearningCardHeaderData header, LearningCardFooterData footer) {
        this();
        this.header = header;
        this.footer = footer;
    }

    public void setHeader(LearningCardHeaderData header) {
        this.header = header;
    }

    public LearningCardHeaderData getHeader() {
        return header;
    }

    public void setFooter(LearningCardFooterData footer) {
        this.footer = footer;
    }

    public LearningCardFooterData getFooter() {
        return footer;
    }

    public void addBlock(LearningCardBlock block) {
        if (block != null) {
            this.blocks.add(block);
        }
    }

    public void addHeading(String text) {
        addBlock(new LearningCardBlock.HeadingBlock(text));
    }

    public void addParagraph(String text) {
        addBlock(new LearningCardBlock.ParagraphBlock(text));
    }

    public void addCodeBlock(String language, String code) {
        addBlock(new LearningCardBlock.CodeBlock(language, code));
    }

    public void addBullet(String text) {
        addBlock(new LearningCardBlock.BulletBlock(text));
    }

    public List<LearningCardBlock> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }

    public List<LearningCardBlock.CodeBlock> getCodeBlocks() {
        return blocks.stream()
                .filter(b -> b instanceof LearningCardBlock.CodeBlock)
                .map(b -> (LearningCardBlock.CodeBlock) b)
                .toList();
    }

    public List<LearningCardBlock.BulletBlock> getBullets() {
        return blocks.stream()
                .filter(b -> b instanceof LearningCardBlock.BulletBlock)
                .map(b -> (LearningCardBlock.BulletBlock) b)
                .toList();
    }

    public void clear() {
        blocks.clear();
    }
}
