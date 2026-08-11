package com.eyecode.language.symbol;

import java.util.List;
import java.util.Optional;

/**
 * A scope in the semantic model (Sprint 5.4a).
 * <p>
 * A scope represents a named region of the program where symbols can
 * be declared and looked up. Scopes form a tree structure rooted at
 * the compilation unit scope.
 */
public interface SymbolScope {

    /**
     * Unique identifier for this scope.
     */
    long id();

    /**
     * Kind of this scope.
     */
    ScopeKind kind();

    /**
     * Parent scope, or empty if this is the root scope.
     */
    Optional<SymbolScope> parent();

    /**
     * All child scopes directly nested in this scope.
     */
    List<SymbolScope> children();

    /**
     * All symbols declared directly in this scope (not inherited).
     */
    List<Symbol> declaredSymbols();

    /**
     * Looks up a symbol by name in this scope only (no parent traversal).
     */
    Optional<Symbol> findLocal(String name);

    /**
     * Looks up a symbol by name, traversing parent scopes if not found locally.
     */
    Optional<Symbol> lookup(String name);

    /**
     * Checks if a symbol with the given name is declared in this scope.
     */
    boolean declares(String name);

    /**
     * Returns the scope id for use as an owner id for symbols declared in this scope.
     */
    default long ownerScopeId() {
        return id();
    }
}