package com.eyecode.language.symbol;

/**
 * Kind of symbol reference (Sprint 5.4a — single {@code SIMPLE} constant;
 * expanded in 5.4b.4 with {@code SIMPLE_NAME} and {@code QUALIFIED_NAME}
 * for the qualified-name foundation).
 * <p>
 * The original {@code SIMPLE} constant is retained for backward
 * compatibility with the batch-resolver model of 5.4b.2; the new
 * constants characterise the *shape* of the reference textually and
 * are used by the qualified-name classifier — they do not yet encode
 * access modes (FIELD_ACCESS, METHOD_CALL, THIS/SUPER access). Those
 * classifications belong to later sprints.
 */
public enum SymbolReferenceKind {
    /**
     * A reference whose kind is not yet classified or is a simple name
     * usage. Retained for backward compatibility with 5.4b.2 — preferred
     * new code should use {@link #SIMPLE_NAME} or {@link #QUALIFIED_NAME}.
     */
    SIMPLE,

    /**
     * A single-component identifier reference such as {@code foo} —
     * no qualifier.
     */
    SIMPLE_NAME,

    /**
     * A qualified reference such as {@code foo.bar}, {@code this.value},
     * {@code super.value}, {@code a.b.c} or {@code pkg.Type} — at least
     * one dot separates the qualifier from the terminal name.
     */
    QUALIFIED_NAME,

    CONSTRUCTOR_CALL
}
