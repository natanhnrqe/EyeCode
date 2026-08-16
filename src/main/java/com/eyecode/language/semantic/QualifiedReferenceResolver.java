package com.eyecode.language.semantic;

import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;
import com.eyecode.language.symbol.SymbolScope;

import java.util.Objects;
import java.util.Optional;

/**
 * Integration entry point that resolves a real
 * {@link SymbolReference} of kind {@link SymbolReferenceKind#QUALIFIED_NAME}
 * through the full qualified-name pipeline (Sprint 5.4b.7).
 * <p>
 * The pipeline is:
 * <pre>
 *   SymbolReference
 *     ↓  dispatch on kind
 *   QualifiedNameClassifier
 *     ↓  syntactic classification (text shape)
 *   QualifiedNameDecomposer
 *     ↓  syntactic decomposition (text → ordered components)
 *   QualifiedNameResolver
 *     ↓  semantic chain (left-to-right via scope + member lookup)
 *   ResolvedSymbol
 * </pre>
 * <p>
 * <b>SIMPLE_NAME vs QUALIFIED_NAME</b> (spec §3): the resolver never
 * <em>guesses</em>. Only references whose kind is
 * {@link SymbolReferenceKind#QUALIFIED_NAME} are accepted here; any
 * other kind ({@code SIMPLE}, {@code SIMPLE_NAME}, etc.) returns
 * {@link QualifiedReferenceResolution.ResolutionStatus#UNRESOLVED} with
 * an empty qualifiedName/qualifierSymbol/resolvedSymbol. Simple
 * references must be routed to {@code JavaNameResolver} by the caller.
 * This is the explicit separation guarantee of the sprint.
 * <p>
 * <b>What it does NOT do</b> (spec §6): imports, static imports,
 * inheritance, interface/superclass resolution, package resolution
 * across files, type inference, overload resolution, method dispatch,
 * generics, field-type lookup. If {@code foo.bar} depends on type
 * information the current {@link com.eyecode.language.symbol.SymbolTable}
 * cannot express (e.g. {@code obj.field} where {@code obj} is a
 * LOCAL_VARIABLE without a member context), the chain yields
 * {@code UNRESOLVED} — no symbol is fabricated.
 * <p>
 * <b>Read-only contract</b>: the resolver never mutates the
 * {@link SymbolScope} / table, the supplied {@link SymbolReference},
 * the {@link QualifiedName}, or any symbol. It only reads the scope
 * chain and produces a fresh immutable
 * {@link QualifiedReferenceResolution}.
 */
public final class QualifiedReferenceResolver {

    /**
     * Resolves a qualified-name {@link SymbolReference} end-to-end.
     * <p>
     * Steps:
     * <ol>
     *   <li>kind check — only {@link SymbolReferenceKind#QUALIFIED_NAME}
     *       proceeds; any other kind returns {@code UNRESOLVED} with
     *       an empty qualified name (spec §3 — no guessing);</li>
     *   <li>classification — {@link QualifiedNameClassifier#classify}
     *       confirms the textual shape is {@code QUALIFIED_NAME} (and
     *       not {@code SIMPLE_NAME} / {@code INVALID});</li>
     *   <li>decomposition — {@link QualifiedNameDecomposer#decompose}
     *       recovers the ordered {@link QualifiedName}; failure yields
     *       {@code UNRESOLVED} with empty qualified name;</li>
     *   <li>resolution — {@link QualifiedNameResolver#resolve} walks
     *       the chain left-to-right via the supplied scope and member
     *       lookup;</li>
     *   <li>wrapping — the {@link QualifiedNameResolution} is wrapped
     *       into a {@link QualifiedReferenceResolution} carrying the
     *       original reference.</li>
     * </ol>
     *
     * @param reference   the {@link SymbolReference} to resolve; must have
     *                    {@link SymbolReferenceKind#QUALIFIED_NAME} kind
     *                    (other kinds yield {@code UNRESOLVED}); never null
     * @param scope       the starting scope for the first lookup; never null
     * @param memberLookup the member-lookup policy; never null
     * @return a {@link QualifiedReferenceResolution}; never null
     * @throws NullPointerException if any argument is null
     */
    public QualifiedReferenceResolution resolve(SymbolReference reference,
                                                SymbolScope scope,
                                                QualifiedMemberLookup memberLookup) {
        Objects.requireNonNull(reference, "reference must not be null");
        Objects.requireNonNull(scope, "scope must not be null");
        Objects.requireNonNull(memberLookup, "memberLookup must not be null");

        if (reference.kind() != SymbolReferenceKind.QUALIFIED_NAME) {
            // Spec §3 — explicit separation. SIMPLE / SIMPLE_NAME go to
            // JavaNameResolver; this resolver does NOT guess. Return an
            // UNRESOLVED with empty qualified name.
            return QualifiedReferenceResolution.unresolved(reference);
        }

        String text = reference.name();
        int baseOffset = reference.range().startOffset();

        QualifiedNameClassifier.Result classification = QualifiedNameClassifier.classify(text, baseOffset);
        if (!classification.isQualifiedName()) {
            // Classifier rejected the textual shape (SIMPLE_NAME, INVALID, etc.).
            return QualifiedReferenceResolution.unresolved(reference);
        }

        Optional<QualifiedName> decomposed = QualifiedNameDecomposer.decompose(text, baseOffset);
        if (decomposed.isEmpty()) {
            // Decomposer rejected the text (whitespace, leading/trailing/double dot, etc.).
            return QualifiedReferenceResolution.unresolved(reference);
        }

        QualifiedName qualifiedName = decomposed.get();
        QualifiedNameResolution inner = new QualifiedNameResolver()
                .resolve(qualifiedName, scope, memberLookup);

        return wrap(reference, qualifiedName, inner);
    }

    /**
     * Wraps a {@link QualifiedNameResolution} into a
     * {@link QualifiedReferenceResolution}, preserving the diagnostic
     * state (qualifier vs terminal, RESOLVED vs UNRESOLVED).
     */
    private static QualifiedReferenceResolution wrap(SymbolReference reference,
                                                    QualifiedName qualifiedName,
                                                    QualifiedNameResolution inner) {
        if (inner.isResolved()) {
            Symbol qualifier = inner.qualifierSymbol().orElseThrow();
            Symbol resolved = inner.resolvedSymbol().orElseThrow();
            return QualifiedReferenceResolution.resolved(
                    reference, qualifiedName, qualifier, resolved);
        }
        // UNRESOLVED — qualifier (if any) is carried in both optionals
        // for diagnostic introspection.
        Symbol qualifier = inner.qualifierSymbol().orElse(null);
        return QualifiedReferenceResolution.unresolved(reference, qualifiedName, qualifier);
    }
}
