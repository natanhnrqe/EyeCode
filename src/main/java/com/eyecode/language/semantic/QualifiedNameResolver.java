package com.eyecode.language.semantic;

import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Qualified-name resolution (Sprint 5.4b.6 — final stage).
 * <p>
 * Resolves a {@link QualifiedName} left-to-right against a starting
 * {@link SymbolScope}, with each subsequent component looked up in the
 * <em>member context</em> of the previously resolved symbol via a
 * {@link QualifiedMemberLookup}. The resolution is sequential:
 * <pre>
 *   foo.bar.baz
 *    ↓   ↓   ↓
 *   foo → bar → baz
 * </pre>
 * <p>
 * <b>Two entry points</b>:
 * <ul>
 *   <li>{@link #resolveQualifier(QualifiedName, SymbolScope)} — resolves
 *       only the leftmost component (the qualifier) using the
 *       hierarchical {@link SymbolScope#lookup(String)}. Kept as a
 *       stable first-stage API (Sprint 5.4b.6 signature).</li>
 *   <li>{@link #resolve(QualifiedName, SymbolScope, QualifiedMemberLookup)}
 *       — full left-to-right chain resolution using the member lookup
 *       policy provided by the caller. Defaults to a
 *       {@link ScopeBasedQualifiedMemberLookup}-compatible policy when
 *       the caller supplies a {@link SymbolTable} indirectly via a
 *       pre-built {@link QualifiedMemberLookup}.</li>
 * </ul>
 * <p>
 * <b>Shadowing of the first component</b> (spec §11): the first lookup
 * uses {@code scope.lookup("foo")} unchanged — the lexically innermost
 * declaration wins (local > parameter > field > type > package). No
 * new shadowing algorithm is introduced.
 * <p>
 * <b>Member lookup does not use the original scope chain</b>
 * (spec §12): once the qualifier is resolved, subsequent components
 * are resolved in the qualifier's own member context (typically
 * {@code qualifier.scopeId()}), not by re-ascending through
 * {@code currentScope.lookup}. This prevents false positives like
 * resolving {@code MyClass.field} to a {@code field} symbol declared
 * inside the enclosing method.
 * <p>
 * <b>Multi-component resolution</b> (spec §9): for {@code foo.bar.baz}
 * the chain is {@code foo → bar → baz}. Any failure short-circuits the
 * chain to {@code UNRESOLVED}, but the last successfully resolved
 * symbol (if any) is preserved in the resulting
 * {@link QualifiedNameResolution#resolvedSymbol()} — this is the
 * diagnostic-preservation rule. There is no new error category
 * exposed.
 * <p>
 * <b>Read-only contract.</b> The resolver never mutates the
 * {@link SymbolScope} / table, the supplied {@link QualifiedName}, or
 * any symbol. It only reads the scope chain and produces a fresh
 * immutable {@link QualifiedNameResolution}.
 */
public final class QualifiedNameResolver {

    /**
     * Resolves only the qualifier (leftmost component) of the given
     * qualified name against the supplied scope.
     * <p>
     * The terminal name (and any intermediate components of a 3+ dot
     * name) is <b>not</b> resolved by this entry point — semantic
     * binding of the terminal against the qualifier's type requires a
     * {@link QualifiedMemberLookup}. For full chain resolution use
     * {@link #resolve(QualifiedName, SymbolScope, QualifiedMemberLookup)}.
     *
     * @param qualifiedName the qualified name whose qualifier is to be
     *                      resolved; must have at least 2 components
     * @param scope         the starting scope for the lookup; never null
     * @return a {@link QualifiedNameResolution}; never null
     * @throws NullPointerException if either argument is null
     */
    public QualifiedNameResolution resolveQualifier(QualifiedName qualifiedName, SymbolScope scope) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(scope, "scope must not be null");

        QualifiedNameComponent qualifierComponent = qualifiedName.qualifier();
        Optional<Symbol> found = scope.lookup(qualifierComponent.name());

        if (found.isPresent()) {
            return QualifiedNameResolution.resolved(
                    qualifiedName,
                    found.get(),
                    found.get());
        }
        return QualifiedNameResolution.unresolved(qualifiedName, null);
    }

    /**
     * Full left-to-right qualified-name resolution (Sprint 5.4b.6 final stage).
     * <p>
     * Resolves each component of {@code qualifiedName} in order:
     * <ol>
     *   <li>the first component is looked up via
     *       {@link SymbolScope#lookup(String)} on the supplied
     *       {@code startingScope};</li>
     *   <li>each subsequent component is looked up via
     *       {@code memberLookup.lookupMember(previous, name)}.</li>
     * </ol>
     * The first lookup failure short-circuits the chain to
     * {@code UNRESOLVED}; the last successfully resolved symbol (if
     * any) is preserved in the result for diagnostic purposes.
     *
     * @param qualifiedName the qualified name to resolve; ≥2 components
     * @param startingScope the starting scope for the first lookup; never null
     * @param memberLookup  the member-lookup policy; never null
     * @return a {@link QualifiedNameResolution}; never null
     * @throws NullPointerException if any argument is null
     */
    public QualifiedNameResolution resolve(QualifiedName qualifiedName,
                                           SymbolScope startingScope,
                                           QualifiedMemberLookup memberLookup) {
        return resolve(qualifiedName, startingScope, memberLookup, QualifiedMemberExpectation.ANY);
    }

    public QualifiedNameResolution resolve(QualifiedName qualifiedName,
                                           SymbolScope startingScope,
                                           QualifiedMemberLookup memberLookup,
                                           QualifiedMemberExpectation terminalExpectation) {
        Objects.requireNonNull(qualifiedName, "qualifiedName must not be null");
        Objects.requireNonNull(startingScope, "startingScope must not be null");
        Objects.requireNonNull(memberLookup, "memberLookup must not be null");
        Objects.requireNonNull(terminalExpectation, "terminalExpectation must not be null");

        List<QualifiedNameComponent> components = qualifiedName.components();

        Symbol first = resolveFirstComponent(components.get(0), startingScope);
        if (first == null) {
            return QualifiedNameResolution.unresolved(qualifiedName, null);
        }

        Symbol current = first;
        for (int i = 1; i < components.size(); i++) {
            String name = components.get(i).name();
            QualifiedMemberExpectation expectation = i == components.size() - 1
                    ? terminalExpectation
                    : QualifiedMemberExpectation.ANY;
            Optional<Symbol> next = memberLookup.lookupMember(current, name, expectation);
            if (next.isEmpty()) {
                return QualifiedNameResolution.unresolved(qualifiedName, first);
            }
            current = next.get();
        }

        return QualifiedNameResolution.resolved(qualifiedName, first, current);
    }

    /**
     * Resolves only the first component via the hierarchical
     * {@link SymbolScope#lookup}. Returns null when no declaration is
     * found (does not throw).
     */
    private static Symbol resolveFirstComponent(QualifiedNameComponent component, SymbolScope scope) {
        Optional<Symbol> found = scope.lookup(component.name());
        return found.orElse(null);
    }

    /**
     * Returns an empty list helper retained for potential future batch
     * entry points — declared as a stub so the {@link QualifiedMemberLookup}
     * parameter list documents the sequential nature of the chain.
     */
    static List<QualifiedNameResolution> emptyBatchPlaceholder() {
        return new ArrayList<>();
    }
}
