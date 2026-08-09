package com.eyecode.language.java.incremental;

/**
 * Immutable record of a safe lexical position in a previous token stream.
 * <p>
 * A checkpoint is a token boundary from which a full re-lex reproduces the
 * same tokens as the previous analysis, together with the token index and the
 * lexical state at that boundary. It never holds a mutable reference to a
 * document snapshot — only plain coordinates.
 *
 * @param version     document version the checkpoint was derived from
 * @param offset      absolute offset (in the old document coordinates)
 * @param tokenIndex  index of the first old token starting at {@code offset}
 * @param state       lexical state at the boundary
 */
public record LexicalCheckpoint(long version, int offset, int tokenIndex, LexicalState state) {

    public LexicalCheckpoint {
        if (offset < 0) {
            throw new IllegalArgumentException("offset must not be negative");
        }
        if (tokenIndex < 0) {
            throw new IllegalArgumentException("tokenIndex must not be negative");
        }
        if (state == null) {
            throw new IllegalArgumentException("state must not be null");
        }
    }
}
