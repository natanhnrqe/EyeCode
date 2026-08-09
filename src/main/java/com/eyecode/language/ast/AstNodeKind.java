package com.eyecode.language.ast;

/**
 * Kinds of AST nodes the parser can produce (Sprint 5.3a).
 * <p>
 * Only kinds that genuinely exist in the current declarative parser are
 * represented — no speculative kinds for future statements/expressions
 * (those belong to 5.3b+).
 */
public enum AstNodeKind {

    COMPILATION_UNIT,
    PACKAGE_DECLARATION,
    IMPORT_DECLARATION,
    CLASS_DECLARATION,
    INTERFACE_DECLARATION,
    ENUM_DECLARATION,
    RECORD_DECLARATION,
    ANNOTATION,
    FIELD_DECLARATION,
    METHOD_DECLARATION,
    CONSTRUCTOR_DECLARATION,
    PARAMETER,
    TYPE,
    MODIFIER
}
