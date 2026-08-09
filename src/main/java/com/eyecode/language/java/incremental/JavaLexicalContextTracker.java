package com.eyecode.language.java.incremental;

import com.eyecode.language.Token;
import com.eyecode.language.java.JavaTokenType;

import java.util.List;

/**
 * Answers lexical-context questions over an immutable token list.
 * <p>
 * Purely lexical: works on the token stream, never parses, never consults an
 * AST or symbol table. The scanner is stateless, so the context that matters
 * for an edit is the token containing (or adjacent to) the edited offset.
 */
public final class JavaLexicalContextTracker {

    /**
     * Index of the token containing {@code offset}. A boundary between two
     * tokens resolves to the token starting there; an offset at the very end
     * resolves to the EOF token.
     */
    public int tokenIndexAt(List<Token> tokens, int offset) {
        if (tokens == null || tokens.isEmpty()) {
            return -1;
        }
        int lo = 0;
        int hi = tokens.size() - 1;
        while (lo <= hi) {
            int mid = (lo + hi) >>> 1;
            Token token = tokens.get(mid);
            if (offset < token.startOffset()) {
                hi = mid - 1;
            } else if (offset >= token.endOffset()) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        if (lo == 0) {
            return 0;
        }
        return Math.min(lo, tokens.size() - 1);
    }

    /**
     * Lexical state at {@code offset} in the old document: NORMAL on a token
     * boundary, or the open context when the offset falls inside a string,
     * character literal or comment.
     */
    public LexicalState stateAt(List<Token> tokens, int offset) {
        if (tokens == null || tokens.isEmpty()) {
            return LexicalState.NORMAL;
        }
        int index = tokenIndexAt(tokens, offset);
        if (index < 0) {
            return LexicalState.NORMAL;
        }
        Token token = tokens.get(index);
        if (offset < token.startOffset() || offset >= token.endOffset()) {
            return LexicalState.NORMAL;
        }
        return stateOf(token);
    }

    /**
     * Safe re-lex start for an edit at {@code editStart}. When the edit falls
     * inside a token the whole token must be re-lexed, so the checkpoint moves
     * back to the token start; when the edit sits on a token boundary the
     * checkpoint moves back to the start of the token <em>ending</em> at that
     * boundary, because the edit can extend that token into the new text
     * (identifier/number continuation, whitespace merge). In both cases the
     * checkpoint offset is a token boundary in the old <em>and</em> the new
     * text, which makes the untouched prefix reusable. The returned checkpoint
     * never references mutable snapshot state.
     */
    public LexicalCheckpoint safeCheckpointBefore(List<Token> tokens, int editStart, long version) {
        if (tokens == null || tokens.isEmpty()) {
            return new LexicalCheckpoint(version, Math.max(0, editStart), 0, LexicalState.NORMAL);
        }
        int index = tokenIndexAt(tokens, editStart);
        if (index < 0) {
            return new LexicalCheckpoint(version, Math.max(0, editStart), 0, LexicalState.NORMAL);
        }
        Token token = tokens.get(index);
        if (editStart == token.startOffset()) {
            if (index == 0) {
                return new LexicalCheckpoint(version, editStart, 0, LexicalState.NORMAL);
            }
            Token previous = tokens.get(index - 1);
            return new LexicalCheckpoint(version, previous.startOffset(), index - 1, LexicalState.NORMAL);
        }
        if (editStart >= token.endOffset()) {
            return new LexicalCheckpoint(version, editStart, index + 1, LexicalState.NORMAL);
        }
        return new LexicalCheckpoint(version, token.startOffset(), index, stateOf(token));
    }

    public boolean isOpenStringBefore(List<Token> tokens, int offset) {
        return stateAt(tokens, offset) == LexicalState.STRING;
    }

    public boolean isOpenBlockCommentBefore(List<Token> tokens, int offset) {
        return stateAt(tokens, offset) == LexicalState.BLOCK_COMMENT;
    }

    private static LexicalState stateOf(Token token) {
        if (token.type() != JavaTokenType.STRING && token.type() != JavaTokenType.CHARACTER
                && token.type() != JavaTokenType.COMMENT) {
            return LexicalState.NORMAL;
        }
        if (token.type() == JavaTokenType.STRING) {
            return LexicalState.STRING;
        }
        if (token.type() == JavaTokenType.CHARACTER) {
            return LexicalState.CHARACTER;
        }
        return token.text().startsWith("//") ? LexicalState.LINE_COMMENT : LexicalState.BLOCK_COMMENT;
    }
}
