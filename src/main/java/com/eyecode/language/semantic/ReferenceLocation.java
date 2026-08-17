package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.symbol.SymbolReference;

import java.util.Objects;

/**
 * The location of a single reference to a symbol in source code (Sprint 5.4d.1).
 * <p>
 * A {@code ReferenceLocation} pairs a {@link SymbolReference} (the canonical
 * semantic occurrence) with the textual {@link TextRange} where it appears.
 * It is the smallest unit returned by {@link ReferenceFinder}; it does NOT
 * carry URI / path / line metadata (the current {@code Symbol} model does
 * not store that information either, so adding it here would fabricate data).
 * <p>
 * The {@code reference} field is the original occurrence preserved verbatim;
 * callers can inspect {@code reference.target()}, {@code reference.scopeId()},
 * {@code reference.kind()} and {@code reference.name()} without losing fidelity.
 * <p>
 * Equality is by value over both fields — two locations are equal iff they
 * carry the same reference and the same range. No file/line metadata is
 * factored in because it does not exist.
 */
public record ReferenceLocation(SymbolReference reference, TextRange range) {

    public ReferenceLocation {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(range, "range must not be null");
    }

    /**
     * Convenience factory that pairs a reference with its own range.
     *
     * @param reference the symbol reference; must be non-null
     * @return a new location carrying the reference and {@code reference.range()}
     */
    public static ReferenceLocation of(SymbolReference reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        return new ReferenceLocation(reference, reference.range());
    }
}
