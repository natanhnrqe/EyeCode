package com.eyecode.language.java;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.java.incremental.IncrementalJavaLexer;
import com.eyecode.language.java.incremental.IncrementalLexResult;

/**
 * {@link LexerService} implementation backed by the incremental Java lexer.
 * <p>
 * Keeps the previous text and lexical snapshot of the last analyzed document
 * (single slot, no multi-document cache) and runs the incremental path when a
 * change can be derived from the texts; otherwise falls back to a full re-lex.
 * The result is always bound to the version of the given snapshot — a stale
 * snapshot lexed out of order produces a stale-versioned result that can never
 * be mistaken for a current one.
 */
public final class JavaLexerService implements LexerService {

    private final IncrementalJavaLexer incrementalLexer = new IncrementalJavaLexer();

    private String previousText;
    private LexerSnapshot previousSnapshot;

    @Override
    public LexerSnapshot lex(DocumentSnapshot document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        String text = document.getText();
        IncrementalLexResult result;
        if (previousSnapshot != null) {
            result = incrementalLexer.lex(previousText, previousSnapshot, text, document.version());
        } else {
            result = incrementalLexer.lex(null, null, text, document.version());
        }
        previousText = text;
        previousSnapshot = result.snapshot();
        return previousSnapshot;
    }
}
