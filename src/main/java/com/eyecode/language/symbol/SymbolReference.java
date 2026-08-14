package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Objects;

/**
 * A reference (usage) of a symbol in source code (Sprint 5.4a, expanded 5.4b.2).
 * <p>
 * This record represents an occurrence where a symbol name is used. A
 * reference carries the <em>name</em> being referenced and the
 * <em>scope</em> where the reference occurs (so a resolver can perform a
 * hierarchical lookup with proper shadowing), plus the source {@link TextRange}.
 * <p>
 * The optional {@code target} field holds a {@link SymbolId} for references
 * that have already been (or are tentatively) bound to a declaration. It
 * may be {@code null} for unresolved references — the {@link SymbolTable}
 * indexing skips such references.
 * <p>
 * The record does not carry resolution logic — it is a structural
 * occurrence that can be resolved by a semantic resolver
 * (see {@code com.eyecode.language.semantic.NameResolver}).
 */
public record SymbolReference(
        SymbolId target,
        TextRange range,
        String name,
        long scopeId,
        SymbolReferenceKind kind
) {

    public SymbolReference {
        Objects.requireNonNull(range, "range must not be null");
        Objects.requireNonNull(name, "name must not be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (kind == null) {
            kind = SymbolReferenceKind.SIMPLE;
        }
    }

    /**
     * Convenience factory for a simple-name reference with no tentative target.
     *
     * @param name    the referenced simple name
     * @param scopeId the scope where the reference occurs (lookup start)
     * @param range   the source range of the reference
     * @return a new reference with {@code SIMPLE} kind and {@code null} target
     */
    public static SymbolReference simple(String name, long scopeId, TextRange range) {
        return new SymbolReference(null, range, name, scopeId, SymbolReferenceKind.SIMPLE);
    }
}
