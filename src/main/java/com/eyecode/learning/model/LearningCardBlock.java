package com.eyecode.learning.model;

public sealed interface LearningCardBlock permits
        LearningCardBlock.HeadingBlock,
        LearningCardBlock.ParagraphBlock,
        LearningCardBlock.CodeBlock,
        LearningCardBlock.BulletBlock {

    record HeadingBlock(String text) implements LearningCardBlock {}

    record ParagraphBlock(String text) implements LearningCardBlock {}

    record CodeBlock(String language, String code) implements LearningCardBlock {}

    record BulletBlock(String text) implements LearningCardBlock {}
}
