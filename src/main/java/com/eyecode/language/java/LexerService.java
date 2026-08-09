package com.eyecode.language.java;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;

/**
 * Official gateway for lexical analysis.
 * <p>
 * Consumes an immutable {@link DocumentSnapshot} and produces an immutable,
 * version-matched {@link LexerSnapshot}. Consumers must depend on this
 * interface and on {@link LexerSnapshot} — never on {@link JavaLexer} — so the
 * implementation can later switch between full re-lex and incremental lexing
 * without touching callers.
 */
public interface LexerService {

    LexerSnapshot lex(DocumentSnapshot document);
}
