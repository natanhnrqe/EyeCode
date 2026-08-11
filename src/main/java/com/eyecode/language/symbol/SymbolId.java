package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Objects;

/**
 * Immutable, deterministic identifier for a symbol (Sprint 5.4a).
 * <p>
 * A SymbolId uniquely identifies a symbol declaration within a semantic
 * model. It is a value object — equality is by value, not by object
 * identity. The id is derived deterministically from the symbol's
 * structural context (owner, declaration range, kind) so that the same
 * symbol reconstructed from the same snapshot always yields the same id.
 * <p>
 * No random UUIDs are used; identity is structural and deterministic.
 */
public final class SymbolId {

    private final long ownerScopeId;
    private final int declarationStart;
    private final int declarationEnd;
    private final SymbolKind kind;

    public SymbolId(long ownerScopeId, TextRange declarationRange, SymbolKind kind) {
        this(ownerScopeId, declarationRange.startOffset(), declarationRange.endOffset(), kind);
    }

    private SymbolId(long ownerScopeId, int declarationStart, int declarationEnd, SymbolKind kind) {
        this.ownerScopeId = ownerScopeId;
        this.declarationStart = declarationStart;
        this.declarationEnd = declarationEnd;
        this.kind = Objects.requireNonNull(kind, "kind must not be null");
    }

    public long ownerScopeId() {
        return ownerScopeId;
    }

    public int declarationStart() {
        return declarationStart;
    }

    public int declarationEnd() {
        return declarationEnd;
    }

    public SymbolKind kind() {
        return kind;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SymbolId that)) return false;
        return ownerScopeId == that.ownerScopeId
                && declarationStart == that.declarationStart
                && declarationEnd == that.declarationEnd
                && kind == that.kind;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ownerScopeId, declarationStart, declarationEnd, kind);
    }

    @Override
    public String toString() {
        return "SymbolId[" + kind + "@" + ownerScopeId + ":" + declarationStart + ".." + declarationEnd + "]";
    }
}