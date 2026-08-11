package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Objects;

/**
 * A reference (usage) of a symbol in source code (Sprint 5.4a).
 * <p>
 * This record represents an occurrence where a symbol name is used.
 * It does not carry resolution logic — it is a structural occurrence
 * that can later be resolved by a semantic resolver.
 */
public record SymbolReference(
        SymbolId target,
        TextRange range,
        SymbolReferenceKind kind
) {

    public SymbolReference {
        Objects.requireNonNull(target, "target must not be null");
        Objects.requireNonNull(range, "range must not be null");
        if (kind == null) {
            kind = SymbolReferenceKind.SIMPLE;
        }
    }
}