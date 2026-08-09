package com.eyecode.language.java.incremental;

import com.eyecode.language.Token;
import com.eyecode.language.java.JavaLexer;
import com.eyecode.language.java.LexerSnapshot;

import java.util.List;

/**
 * Official full re-lex fallback.
 * <p>
 * Re-tokenizes a complete text with the pure {@link JavaLexer}. The
 * incremental lexer falls back to this strategy whenever it cannot prove that
 * incremental reuse is safe — correctness always wins over optimization.
 */
public final class FullRelexStrategy {

    private final JavaLexer lexer = new JavaLexer();

    public List<Token> lexTokens(String text) {
        return lexer.tokenize(text);
    }

    public LexerSnapshot lex(String text, long version) {
        return new LexerSnapshot(version, lexTokens(text));
    }
}
