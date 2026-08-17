package com.eyecode.language.semantic;

import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolTable;

import java.util.List;

/**
 * Core abstraction for "Find References" — given a {@link Symbol}, return every
 * occurrence that resolves to it (Sprint 5.4d.1).
 * <p>
 * The contract is read-only: implementations must not mutate the
 * {@link SymbolTable}, its scopes, the {@link Symbol} or any
 * {@code SymbolReference}. They consume the references already indexed in
 * the table; they do NOT walk the AST, do NOT perform textual search and
 * do NOT consult a workspace-wide index.
 * <p>
 * If the underlying {@code SymbolTable} does not index enough references to
 * satisfy a real-world query, the implementation returns only the references
 * that are available and the gap is documented — no second-best indexer is
 * fabricated (spec §11). A subsequent sprint (5.4d.2) will plug a real
 * reference collector into the table builder.
 * <p>
 * Implementations are expected to:
 * <ul>
 *   <li>reject null arguments;</li>
 *   <li>return an empty list (never {@code null}) when the symbol has no
 *       references;</li>
 *   <li>return a deterministically-ordered, deduplicated list (sort by
 *       {@code range.startOffset}, then {@code range.endOffset}, then
 *       {@code kind} tie-break — no file identity is invented).</li>
 * </ul>
 */
public interface ReferenceFinder {

    /**
     * Returns every indexed reference whose {@code target} matches the given
     * symbol's {@code id}.
     * <p>
     * Identity is structural: two {@code Symbol}s are considered "the same"
     * iff their {@code SymbolId} fields are equal (no name-based matching,
     * no textual search). This guarantees correct shadowing behavior — a
     * local variable named {@code value} never matches a field named
     * {@code value}.
     *
     * @param symbol the symbol to look up; must be non-null
     * @param table  the table to consult; must be non-null
     * @return an immutable, deterministic, deduplicated list of references;
     *         never {@code null}, possibly empty
     * @throws NullPointerException if {@code symbol} or {@code table} is null
     */
    List<ReferenceLocation> findReferences(Symbol symbol, SymbolTable table);
}
