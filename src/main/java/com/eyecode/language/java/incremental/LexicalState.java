package com.eyecode.language.java.incremental;

/**
 * Minimal lexical context states used by the incremental lexer.
 * <p>
 * A state describes whether a given offset sits inside an open lexical
 * construct that can make an edit affect tokens far beyond its own position.
 * This is lexical context only — never semantic state, never AST.
 */
public enum LexicalState {

    NORMAL,
    STRING,
    CHARACTER,
    LINE_COMMENT,
    BLOCK_COMMENT;

    public boolean isOpenContext() {
        return this != NORMAL;
    }
}
