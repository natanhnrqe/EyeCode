package com.eyecode.language.semantic;

import com.eyecode.language.symbol.Symbol;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of qualified-name resolution (Sprint 5.4b.6 — final stage).
 * <p>
 * Carries the outcome of resolving a {@link QualifiedName} left-to-right
 * against a starting {@link com.eyecode.language.symbol.SymbolScope}. The
 * resolution proceeds component by component:
 * <ol>
 *   <li>the leftmost component (the <em>qualifier</em>) is looked up in
 *       the starting scope using the hierarchical lookup of
 *       {@link com.eyecode.language.symbol.SymbolScope#lookup(String)};</li>
 *   <li>each subsequent component is looked up in the <em>member context</em>
 *       of the previously resolved symbol via a
 *       {@link QualifiedMemberLookup};</li>
 *   <li>the last successfully resolved component becomes
 *       {@link #resolvedSymbol()}.</li>
 * </ol>
 * <p>
 * <b>Fields</b>:
 * <ul>
 *   <li>{@link #qualifiedName()} — the original qualified name (always
 *       carried back, including unresolved components);</li>
 *   <li>{@link #qualifierSymbol()} — the symbol resolved for the
 *       <em>first</em> component, if any. Empty when the first lookup
 *       itself fails. Even on multi-component partial failure this carries
 *       the leftmost resolved symbol (which may equal
 *       {@link #resolvedSymbol()} when only one component resolved);</li>
 *   <li>{@link #resolvedSymbol()} — the symbol resolved for the
 *       <em>last successfully resolved</em> component. On full success
 *       this is the terminal-name symbol. On failure at the first step
 *       this is empty. On partial success (e.g. {@code foo} resolved but
 *       {@code bar} missing) this still carries {@code foo} so callers
 *       can introspect the last known state — this is the
 *       diagnostic-preservation guarantee of the 5.4b.6 spec;</li>
 *   <li>{@link #status()} — the outcome, one of
 *       {@link ResolutionStatus#RESOLVED} or
 *       {@link ResolutionStatus#UNRESOLVED}.</li>
 * </ul>
 * <p>
 * Exactly two statuses are exposed: {@link ResolutionStatus#RESOLVED}
 * (every component resolved) and {@link ResolutionStatus#UNRESOLVED}
 * (the chain stopped at some point; the most-progressed resolved symbol,
 * if any, is preserved in {@link #resolvedSymbol()}). There is
 * intentionally no {@code AMBIGUOUS}, no {@code INACCESSIBLE} and no
 * {@code WRONG_KIND} — those belong to later sprints.
 * <p>
 * Immutable and thread-safe. Equality is by value (all four fields).
 */
public final class QualifiedNameResolution {

    /**
     * Outcome of qualified-name resolution.
     */
    public enum ResolutionStatus {
        /**
         * Every component of the qualified name was resolved — the
         * terminal-name symbol is carried in {@link #resolvedSymbol()}.
         */
        RESOLVED,
        /**
         * Resolution stopped before reaching the terminal component. The
         * {@link #resolvedSymbol()} optional may still carry the
         * leftmost resolved symbol so callers can introspect the last
         * known progress (spec §9 — "preservar o último símbolo resolvido
         * internamente para facilitar diagnóstico futuro").
         */
        UNRESOLVED
    }

    private final QualifiedName qualifiedName;
    private final Optional<Symbol> qualifierSymbol;
    private final Optional<Symbol> resolvedSymbol;
    private final ResolutionStatus status;

    private QualifiedNameResolution(QualifiedName qualifiedName,
                                    Optional<Symbol> qualifierSymbol,
                                    Optional<Symbol> resolvedSymbol,
                                    ResolutionStatus status) {
        this.qualifiedName = qualifiedName;
        this.qualifierSymbol = qualifierSymbol;
        this.resolvedSymbol = resolvedSymbol;
        this.status = status;
    }

    /**
     * Builds a {@link QualifiedNameResolution} for a successful chain —
     * both the qualifier and the terminal resolved.
     *
     * @param qualifiedName    the original qualified name; never null
     * @param qualifierSymbol  the resolved qualifier symbol; never null
     * @param resolvedSymbol   the resolved terminal symbol; never null
     * @return a new {@link QualifiedNameResolution} with status RESOLVED
     */
    public static QualifiedNameResolution resolved(QualifiedName qualifiedName,
                                                   Symbol qualifierSymbol,
                                                   Symbol resolvedSymbol) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(qualifierSymbol, "qualifierSymbol must not be null");
        Objects.requireNonNull(resolvedSymbol, "resolvedSymbol must not be null");
        return new QualifiedNameResolution(
                qualifiedName,
                Optional.of(qualifierSymbol),
                Optional.of(resolvedSymbol),
                ResolutionStatus.RESOLVED);
    }

    /**
     * Builds a {@link QualifiedNameResolution} for a chain that did not
     * resolve all the way to the terminal. Carries the leftmost resolved
     * symbol in both {@link #qualifierSymbol()} and {@link #resolvedSymbol()}
     * (when at least the qualifier was found) so callers can introspect
     * the last successful step.
     *
     * @param qualifiedName   the original qualified name; never null
     * @param qualifierSymbol the leftmost resolved symbol, or null when
     *                        the very first lookup failed
     * @return a new {@link QualifiedNameResolution} with status UNRESOLVED
     */
    public static QualifiedNameResolution unresolved(QualifiedName qualifiedName,
                                                     Symbol qualifierSymbol) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        return new QualifiedNameResolution(
                qualifiedName,
                Optional.ofNullable(qualifierSymbol),
                Optional.ofNullable(qualifierSymbol),
                ResolutionStatus.UNRESOLVED);
    }

    /**
     * The original qualified name being resolved.
     *
     * @return the qualified name; never null
     */
    public QualifiedName qualifiedName() {
        return qualifiedName;
    }

    /**
     * The resolved qualifier symbol (leftmost component), if any.
     * <p>
     * Present iff the first lookup succeeded. On a multi-component
     * partial failure this carries the leftmost resolved symbol.
     *
     * @return the qualifier symbol; never null but possibly empty
     */
    public Optional<Symbol> qualifierSymbol() {
        return qualifierSymbol;
    }

    /**
     * The last successfully resolved symbol.
     * <p>
     * On full success this is the terminal-name symbol. On partial
     * success (some component failed) this carries the most-progressed
     * resolved symbol so callers can introspect the last known step.
     * On a total failure (the very first lookup failed) this is empty.
     *
     * @return the last resolved symbol; never null but possibly empty
     */
    public Optional<Symbol> resolvedSymbol() {
        return resolvedSymbol;
    }

    /**
     * The terminal-name component of {@link #qualifiedName()}.
     * <p>
     * Convenience accessor — equivalent to {@code qualifiedName().terminalName()}.
     * Always a syntactic component; only resolved by the chain when
     * {@link #status()} is {@link ResolutionStatus#RESOLVED}.
     *
     * @return the terminal-name component; never null
     */
    public QualifiedNameComponent terminalName() {
        return qualifiedName.terminalName();
    }

    /**
     * The outcome of the resolution.
     *
     * @return the resolution status; never null
     */
    public ResolutionStatus status() {
        return status;
    }

    /**
     * Convenience predicate: is this result {@link ResolutionStatus#RESOLVED}?
     */
    public boolean isResolved() {
        return status == ResolutionStatus.RESOLVED;
    }

    /**
     * Convenience predicate: is this result {@link ResolutionStatus#UNRESOLVED}?
     */
    public boolean isUnresolved() {
        return status == ResolutionStatus.UNRESOLVED;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QualifiedNameResolution that)) return false;
        return qualifiedName.equals(that.qualifiedName)
                && qualifierSymbol.equals(that.qualifierSymbol)
                && resolvedSymbol.equals(that.resolvedSymbol)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifiedName, qualifierSymbol, resolvedSymbol, status);
    }

    @Override
    public String toString() {
        return "QualifiedNameResolution[status=" + status
                + ", qualifiedName=" + qualifiedName
                + ", qualifierSymbol=" + symbolName(qualifierSymbol)
                + ", resolvedSymbol=" + symbolName(resolvedSymbol)
                + "]";
    }

    private static String symbolName(Optional<Symbol> opt) {
        return opt.isPresent() ? opt.get().name() : "<empty>";
    }
}
