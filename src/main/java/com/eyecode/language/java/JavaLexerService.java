package com.eyecode.language.java;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.Token;

import java.util.List;

/**
 * Full re-lex implementation of {@link LexerService}.
 * <p>
 * Stateless: every call re-tokenizes the whole snapshot text with the pure
 * {@link JavaLexer} and binds the result to the snapshot's version. No caching,
 * no incremental logic — intentionally deferred to Sprint 5.2c.
 */
public final class JavaLexerService implements LexerService {

    private final JavaLexer lexer = new JavaLexer();

    @Override
    public LexerSnapshot lex(DocumentSnapshot document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        List<Token> tokens = lexer.tokenize(document.getText());
        return new LexerSnapshot(document.version(), tokens);
    }
}
