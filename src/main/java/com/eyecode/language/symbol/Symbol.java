package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Objects;
import java.util.Set;

/**
 * Canonical, immutable symbol in the semantic model (Sprint 5.4a).
 * <p>
 * A symbol represents a single declaration in the source code. It is
 * the fundamental unit of the semantic model — the output of the
 * symbol table builder. All fields are immutable and null-safe.
 * <p>
 * The {@code ownerScopeId} is the scope where this symbol is declared.
 * The {@code scopeId} is the scope where this symbol's members are
 * declared (for types, this is the type's own scope; for other symbols,
 * it equals {@code ownerScopeId}).
 */
public record Symbol(
        SymbolId id,
        SymbolKind kind,
        String name,
        TextRange declarationRange,
        long ownerScopeId,
        long scopeId,
        String qualifiedName,
        Set<SymbolModifier> modifiers
) {

    public Symbol {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(kind, "kind must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(declarationRange, "declarationRange must not be null");
        if (scopeId == 0) {
            scopeId = ownerScopeId;
        }
        modifiers = Set.copyOf(Objects.requireNonNull(modifiers, "modifiers must not be null"));
    }

    public Symbol(SymbolId id, SymbolKind kind, String name, TextRange declarationRange,
                  long ownerScopeId, long scopeId, String qualifiedName) {
        this(id, kind, name, declarationRange, ownerScopeId, scopeId, qualifiedName, Set.of());
    }

    /**
     * Returns the scope where this symbol's members are declared.
     * For types, this is the type's own scope; for other symbols,
     * it equals {@link #ownerScopeId()}.
     */
    public long scopeId() {
        return scopeId;
    }

    public String ownerQualifiedName() {
        return qualifiedName != null ? qualifiedName.substring(0, qualifiedName.lastIndexOf('.')) : "";
    }

    public String simpleName() {
        return name;
    }
}
