package com.eyecode.language.cfg;

/**
 * Kind of control-flow edge (Sprint 5.3d).
 * <p>
 * Every edge in a {@link ControlFlowGraph} is labelled with exactly one of
 * these kinds. The edge kind tells analyses which property of the
 * destination block is reachable through it (normal fall-through, a
 * condition outcome, an abrupt terminator).
 */
public enum ControlFlowEdgeKind {

    /**
     * Plain sequential control flow — the destination follows the source
     * unconditionally.
     */
    NORMAL,

    /**
     * Edge taken when a condition evaluates to {@code true} (if/while/for
     * condition, case label match).
     */
    TRUE,

    /**
     * Edge taken when a condition evaluates to {@code false} (if/while/for
     * condition that falls through after the body).
     */
    FALSE,

    /**
     * Edge taken by a {@code break} statement (terminates the innermost
     * loop or switch and transfers control to the enclosing exit).
     */
    BREAK,

    /**
     * Edge taken by a {@code continue} statement (transfers control back to
     * the loop header).
     */
    CONTINUE,

    /**
     * Edge taken by a {@code return} statement (exits the enclosing
     * method/constructor — connected to the synthetic exit block).
     */
    RETURN,

    /**
     * Edge taken by a {@code throw} statement (exits the enclosing method
     * abnormally — connected to the synthetic exit block, or to a catch
     * block when one exists in scope).
     */
    THROW
}
