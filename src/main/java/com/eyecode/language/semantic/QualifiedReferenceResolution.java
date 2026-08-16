package com.eyecode.language.semantic;

import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolReference;

import java.util.Objects;
import java.util.Optional;

/**
 * Immutable result of qualified-reference resolution (Sprint 5.4b.7).
 * <p>
 * Carries the outcome of resolving a real {@link SymbolReference} of kind
 * {@link com.eyecode.language.symbol.SymbolReferenceKind#QUALIFIED_NAME}
 * through the qualified-reference pipeline:
 * <pre>
 *   SymbolReference
 *     ↓
 *   QualifiedNameClassifier
 *     ↓
 *   QualifiedNameDecomposer
 *     ↓
 *   QualifiedNameResolver
 *     ↓
 *   ResolvedSymbol
 * </pre>
 * <p>
 * <b>Fields</b>:
 * <ul>
 *   <li>{@link #reference()} — the original {@link SymbolReference}
 *       (always carried back, including unresolved cases);</li>
 *   <li>{@link #qualifiedName()} — the decomposed
 *       {@link QualifiedName}; empty when the input could not be
 *       decomposed (e.g. invalid text or non-qualified reference);</li>
 *   <li>{@link #qualifierSymbol()} — the symbol resolved for the
 *       leftmost component, if any;</li>
 *   <li>{@link #resolvedSymbol()} — the last successfully resolved
 *       symbol (terminal on full success, qualifier on partial
 *       failure);</li>
 *   <li>{@link #status()} — {@link ResolutionStatus#RESOLVED} or
 *       {@link ResolutionStatus#UNRESOLVED}.</li>
 * </ul>
 * <p>
 * Reuses {@link QualifiedNameResolution.ResolutionStatus} — the two
 * statuses ({@code RESOLVED} / {@code UNRESOLVED}) carry the same
 * semantics across the qualified-resolution chain. No new diagnostic
 * categories are introduced this sprint
 * ({@code AMBIGUOUS} / {@code INACCESSIBLE} / {@code WRONG_KIND} belong
 * to later sprints).
 * <p>
 * Immutable and thread-safe. Equality is by value (all five fields).
 */
public final class QualifiedReferenceResolution {

    /**
     * Outcome of qualified-reference resolution.
     */
    public enum ResolutionStatus {
        /**
         * Every component of the qualified reference was resolved —
         * the terminal-name symbol is carried in
         * {@link #resolvedSymbol()}.
         */
        RESOLVED,
        /**
         * Resolution stopped before reaching the terminal component,
         * or the input could not be classified/decomposed, or the
         * input kind is not {@link com.eyecode.language.symbol.SymbolReferenceKind#QUALIFIED_NAME}.
         * The {@link #resolvedSymbol()} optional may still carry the
         * leftmost resolved symbol so callers can introspect the last
         * known progress.
         */
        UNRESOLVED
    }

    private final SymbolReference reference;
    private final Optional<QualifiedName> qualifiedName;
    private final Optional<Symbol> qualifierSymbol;
    private final Optional<Symbol> resolvedSymbol;
    private final ResolutionStatus status;

    private QualifiedReferenceResolution(SymbolReference reference,
                                         Optional<QualifiedName> qualifiedName,
                                         Optional<Symbol> qualifierSymbol,
                                         Optional<Symbol> resolvedSymbol,
                                         ResolutionStatus status) {
        this.reference = reference;
        this.qualifiedName = qualifiedName;
        this.qualifierSymbol = qualifierSymbol;
        this.resolvedSymbol = resolvedSymbol;
        this.status = status;
    }

    /**
     * Builds a {@link QualifiedReferenceResolution} for a successful
     * end-to-end resolution.
     *
     * @param reference        the original reference; never null
     * @param qualifiedName    the decomposed qualified name; never null
     * @param qualifierSymbol  the resolved qualifier symbol; never null
     * @param resolvedSymbol   the resolved terminal symbol; never null
     * @return a new {@link QualifiedReferenceResolution} with status RESOLVED
     */
    public static QualifiedReferenceResolution resolved(SymbolReference reference,
                                                       QualifiedName qualifiedName,
                                                       Symbol qualifierSymbol,
                                                       Symbol resolvedSymbol) {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(qualifierSymbol, "qualifierSymbol must not be null");
        Objects.requireNonNull(resolvedSymbol, "resolvedSymbol must not be null");
        return new QualifiedReferenceResolution(
                reference,
                Optional.of(qualifiedName),
                Optional.of(qualifierSymbol),
                Optional.of(resolvedSymbol),
                ResolutionStatus.RESOLVED);
    }

    /**
     * Builds a {@link QualifiedReferenceResolution} for a partial
     * resolution (chain stopped before the terminal). Carries the
     * leftmost resolved symbol in both {@link #qualifierSymbol()} and
     * {@link #resolvedSymbol()} when at least one component was found,
     * so callers can introspect the last successful step (spec §9).
     *
     * @param reference       the original reference; never null
     * @param qualifiedName   the decomposed qualified name; never null
     * @param qualifierSymbol the leftmost resolved symbol, or null when
     *                        the very first lookup failed
     * @return a new {@link QualifiedReferenceResolution} with status
     *         UNRESOLVED
     */
    public static QualifiedReferenceResolution unresolved(SymbolReference reference,
                                                         QualifiedName qualifiedName,
                                                         Symbol qualifierSymbol) {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        return new QualifiedReferenceResolution(
                reference,
                Optional.of(qualifiedName),
                Optional.ofNullable(qualifierSymbol),
                Optional.ofNullable(qualifierSymbol),
                ResolutionStatus.UNRESOLVED);
    }

    /**
     * Builds a {@link QualifiedReferenceResolution} for an
     * UNRESOLVED outcome that produced no {@link QualifiedName} (the
     * input could not be classified or decomposed, or the reference
     * kind was not {@code QUALIFIED_NAME}).
     *
     * @param reference the original reference; never null
     * @return a new {@link QualifiedReferenceResolution} with status
     *         UNRESOLVED and empty qualifiedName/qualifierSymbol/resolvedSymbol
     */
    public static QualifiedReferenceResolution unresolved(SymbolReference reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        return new QualifiedReferenceResolution(
                reference,
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                ResolutionStatus.UNRESOLVED);
    }

    /**
     * The original {@link SymbolReference} that was resolved.
     *
     * @return the reference; never null
     */
    public SymbolReference reference() {
        return reference;
    }

    /**
     * The decomposed {@link QualifiedName}.
     * <p>
     * Present when the input was successfully classified and
     * decomposed. Empty when the input could not be classified or
     * decomposed, or when the reference kind was not
     * {@code QUALIFIED_NAME}.
     *
     * @return the qualified name; never null but possibly empty
     */
    public Optional<QualifiedName> qualifiedName() {
        return qualifiedName;
    }

    /**
     * The resolved qualifier symbol (leftmost component), if any.
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
     *
     * @return the last resolved symbol; never null but possibly empty
     */
    public Optional<Symbol> resolvedSymbol() {
        return resolvedSymbol;
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
        if (!(o instanceof QualifiedReferenceResolution that)) return false;
        return reference.equals(that.reference)
                && qualifiedName.equals(that.qualifiedName)
                && qualifierSymbol.equals(that.qualifierSymbol)
                && resolvedSymbol.equals(that.resolvedSymbol)
                && status == that.status;
    }

    @Override
    public int hashCode() {
        return Objects.hash(reference, qualifiedName, qualifierSymbol, resolvedSymbol, status);
    }

    @Override
    public String toString() {
        return "QualifiedReferenceResolution[status=" + status
                + ", reference=" + reference
                + ", qualifiedName=" + (qualifiedName.isPresent()
                        ? qualifiedName.get() : "<empty>")
                + ", qualifierSymbol=" + symbolName(qualifierSymbol)
                + ", resolvedSymbol=" + symbolName(resolvedSymbol)
                + "]";
    }

    private static String symbolName(Optional<Symbol> opt) {
        return opt.isPresent() ? opt.get().name() : "<empty>";
    }
}
