package com.eyecode.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;

/**
 * Official gateway for syntactic analysis.
 * <p>
 * Consumes an immutable {@link DocumentSnapshot} and produces an immutable,
 * version-matched {@link ParserSnapshot}. Consumers must depend on this
 * interface and on {@link ParserSnapshot} — never on the parser classes —
 * so the implementation can later switch between full parse and incremental
 * parse without touching callers.
 *
 * <h2>Contract</h2>
 * <ul>
 *   <li>{@code parse(...)} never returns {@code null};</li>
 *   <li>the returned snapshot version equals the input document version;</li>
 *   <li>the snapshot's AST describes exactly the document's text — no
 *       stale or partial trees;</li>
 *   <li>the service is safe to call concurrently for distinct sessions
 *       (the default implementation serializes per-session via a
 *       single-slot cache).</li>
 * </ul>
 */
public interface ParserService {

    ParserSnapshot parse(DocumentSnapshot document);
}
