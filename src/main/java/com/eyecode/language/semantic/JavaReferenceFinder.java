package com.eyecode.language.semantic;

import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolTable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Default {@link ReferenceFinder} implementation that consumes only the
 * references already indexed in the supplied {@link SymbolTable} (Sprint 5.4d.1).
 * <p>
 * The finder is a thin wrapper over {@link SymbolTable#referencesTo} — it does
 * NO AST walk, NO textual search, NO name-based matching. Identity is structural:
 * a reference is included iff {@code reference.target().equals(symbol.id())}.
 * This is what makes the implementation correct under shadowing: a field named
 * {@code value} and a local variable named {@code value} have different
 * {@code SymbolId}s (different {@code ownerScopeId}, different
 * {@code declarationRange}), so the field's finder never returns the local's
 * references and vice versa.
 * <p>
 * Limitations (documented, deferred to 5.4d.2):
 * <ul>
 *   <li>The current {@code SymbolTableBuilder} does not call
 *       {@code ProjectSymbolTable.addReference}, so production queries return
 *       an empty list. The contract is implemented; the population is the next
 *       sprint.</li>
 *   <li>No file identity is invented — sorting falls back to
 *       {@code (startOffset, endOffset, kind.ordinal())}.</li>
 * </ul>
 * The implementation is thread-safe (no mutable state on the instance) and
 * read-only (no mutation of any input).
 */
public final class JavaReferenceFinder implements ReferenceFinder {

    private static final Comparator<ReferenceLocation> RANGE_COMPARATOR =
            Comparator.comparingInt((ReferenceLocation loc) -> loc.range().startOffset())
                    .thenComparingInt(loc -> loc.range().endOffset())
                    .thenComparingInt(loc -> loc.reference().kind().ordinal());

    @Override
    public List<ReferenceLocation> findReferences(Symbol symbol, SymbolTable table) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(table, "table must not be null");

        List<SymbolReference> indexed = table.referencesTo(symbol.id());
        if (indexed.isEmpty()) {
            return List.of();
        }

        Set<ReferenceLocation> dedup = new LinkedHashSet<>(indexed.size());
        for (SymbolReference ref : indexed) {
            if (ref.target() == null || !ref.target().equals(symbol.id())) {
                continue;
            }
            dedup.add(ReferenceLocation.of(ref));
        }

        List<ReferenceLocation> sorted = new ArrayList<>(dedup);
        sorted.sort(RANGE_COMPARATOR);
        return List.copyOf(sorted);
    }
}
