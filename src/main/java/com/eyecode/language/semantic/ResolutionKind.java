package com.eyecode.language.semantic;

/**
 * Kind of name resolution result (Sprint 5.4b.1).
 */
public enum ResolutionKind {

    /**
     * The reference was successfully resolved to a symbol.
     */
    RESOLVED,

    /**
     * The reference could not be resolved to any symbol.
     */
    UNRESOLVED,

    /**
     * The reference matched multiple symbols (ambiguous).
     */
    AMBIGUOUS
}