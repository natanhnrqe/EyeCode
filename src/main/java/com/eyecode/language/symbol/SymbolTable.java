package com.eyecode.language.symbol;

import com.eyecode.language.ast.AstNode;

import java.util.List;
import java.util.Optional;

/**
 * Interface for a symbol table — the core lookup structure of the semantic model (Sprint 5.4a).
 * <p>
 * A symbol table provides lookup operations over all symbols in a semantic
 * model. It is an immutable view; modifications are done via the builder
 * and then a new immutable snapshot is produced.
 */
public interface SymbolTable {

    /**
     * Finds a symbol by its unique identifier.
     */
    Optional<Symbol> find(SymbolId id);

    /**
     * Finds a symbol by name in a specific scope (local lookup only).
     */
    Optional<Symbol> findByName(long scopeId, String name);

    /**
     * Looks up a symbol by name starting from a scope, traversing parents.
     */
    Optional<Symbol> lookup(long scopeId, String name);

    /**
     * Returns all symbols declared directly in the given scope.
     */
    List<Symbol> symbolsIn(long scopeId);

    /**
     * Retrieves a scope by its id.
     */
    Optional<SymbolScope> scope(long scopeId);

    /**
     * Returns all references to the given symbol.
     */
    List<SymbolReference> referencesTo(SymbolId symbolId);

    /**
     * Returns the root scope of this symbol table.
     */
    SymbolScope rootScope();
}