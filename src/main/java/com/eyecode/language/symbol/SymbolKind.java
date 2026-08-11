package com.eyecode.language.symbol;

/**
 * Kinds of symbols in the canonical symbol model (Sprint 5.4a).
 * <p>
 * This enum represents the minimal set of symbol categories needed for
 * declaration indexing. It does NOT include semantic concepts like
 * resolved types, overloaded members, or inherited members — those
 * belong to later sprints.
 */
public enum SymbolKind {

    PACKAGE,
    TYPE,
    INTERFACE,
    ENUM,
    ANNOTATION,
    FIELD,
    METHOD,
    CONSTRUCTOR,
    PARAMETER,
    LOCAL_VARIABLE,
    TYPE_PARAMETER
}