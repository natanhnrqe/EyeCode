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
 * <p>
 * Contract (Sprint 5.2e):
 * <ul>
 *   <li>{@code lex(...)} never returns {@code null};</li>
 *   <li>the returned snapshot version equals the input document version;</li>
 *   <li>tokens and the token list are immutable — no mutable state leaks;</li>
 *   <li>anonymous one-shot snapshots ({@code sessionId() == null}, e.g.
 *       {@code DocumentSnapshot.oneShot}) are full-lexed and never cached;</li>
 *   <li>session snapshots may be served from cache, reused, or incrementally
 *       re-lexed, respecting version monotonicity;</li>
 *   <li>a stale snapshot (older version than the cached one) is lexed
 *       correctly for its own version but never overwrites the newer cached
 *       entry (newest wins).</li>
 * </ul>
 */
public interface LexerService {

    LexerSnapshot lex(DocumentSnapshot document);
}
