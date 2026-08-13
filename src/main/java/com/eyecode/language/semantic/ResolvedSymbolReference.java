package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.symbol.SymbolId;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;

import java.util.Objects;

/**
 * Result of resolving a symbol reference (Sprint 5.4b.1).
 * <p>
 * Carries the original reference, the resolution outcome, and the
 * resolved symbol id when applicable.
 * <p>
 * Immutable and thread-safe.
 */
public final class ResolvedSymbolReference {

    private final SymbolReference originalReference;
    private final ResolutionKind resolutionKind;
    private final SymbolId resolvedSymbolId;

    public ResolvedSymbolReference(SymbolReference originalReference,
                                   ResolutionKind resolutionKind,
                                   SymbolId resolvedSymbolId) {
        this.originalReference = Objects.requireNonNull(originalReference, "originalReference must not be null");
        this.resolutionKind = Objects.requireNonNull(resolutionKind, "resolutionKind must not be null");
        this.resolvedSymbolId = resolvedSymbolId;
    }

    public SymbolReference originalReference() {
        return originalReference;
    }

    public ResolutionKind resolutionKind() {
        return resolutionKind;
    }

    public SymbolId resolvedSymbolId() {
        return resolvedSymbolId;
    }

    public boolean isResolved() {
        return resolutionKind == ResolutionKind.RESOLVED;
    }

    public boolean isUnresolved() {
        return resolutionKind == ResolutionKind.UNRESOLVED;
    }

    public boolean isAmbiguous() {
        return resolutionKind == ResolutionKind.AMBIGUOUS;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolvedSymbolReference that)) return false;
        return originalReference.equals(that.originalReference)
                && resolutionKind == that.resolutionKind
                && Objects.equals(resolvedSymbolId, that.resolvedSymbolId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(originalReference, resolutionKind, resolvedSymbolId);
    }

    @Override
    public String toString() {
        return "ResolvedSymbolReference[" + resolutionKind + "]";
    }

    /**
     * Creates a resolved result.
     */
    public static ResolvedSymbolReference resolved(SymbolReference ref, SymbolId id) {
        return new ResolvedSymbolReference(ref, ResolutionKind.RESOLVED, id);
    }

    /**
     * Creates an unresolved result.
     */
    public static ResolvedSymbolReference unresolved(SymbolReference ref) {
        return new ResolvedSymbolReference(ref, ResolutionKind.UNRESOLVED, null);
    }

    /**
     * Creates an ambiguous result.
     */
    public static ResolvedSymbolReference ambiguous(SymbolReference ref) {
        return new ResolvedSymbolReference(ref, ResolutionKind.AMBIGUOUS, null);
    }
}