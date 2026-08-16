package com.eyecode.language.semantic;

import com.eyecode.language.symbol.Symbol;

import java.util.Optional;

/**
 * Abstraction for member lookup in a qualified-name chain
 * (Sprint 5.4b.6).
 * <p>
 * Given a {@link Symbol} acting as a qualifier (the leftmost resolved
 * component of a {@link QualifiedName}) and a member name, returns the
 * {@link Symbol} for that member in the qualifier's own context — or
 * empty when no such member exists in the current model.
 * <p>
 * The interface intentionally abstracts the <em>policy</em> of how a
 * qualifier's members are located: implementations decide whether the
 * qualifier kind (TYPE / INTERFACE / ENUM / ANNOTATION / PACKAGE /
 * METHOD / FIELD / PARAMETER / LOCAL_VARIABLE) supports member lookup
 * at all, and where to look (the qualifier's own scope, an inferred
 * type, …). This sprint supplies a single default implementation
 * ({@code ScopeBasedQualifiedMemberLookup}) that supports only the
 * kinds the current {@link com.eyecode.language.symbol.SymbolTable} can
 * express structurally — type members (TYPE / INTERFACE / ENUM /
 * ANNOTATION) via {@code symbol.scopeId()} and package members
 * (PACKAGE) via the package's own scope. Future sprints can plug in
 * type-inference, inheritance or static-import policies behind this
 * same interface.
 * <p>
 * The contract is read-only: implementations must not mutate the
 * qualifier or any underlying state.
 */
public interface QualifiedMemberLookup {

    /**
     * Looks up a member {@code name} in the context of the given
     * {@code qualifier}.
     * <p>
     * Returns the resolved member {@link Symbol} when one is found in
     * the qualifier's supported context, or empty when:
     * <ul>
     *   <li>the qualifier's kind does not support member lookup under
     *       the current implementation (e.g. FIELD / PARAMETER /
     *       LOCAL_VARIABLE without type information);</li>
     *   <li>the qualifier exists but has no such member;</li>
     *   <li>the qualifier's member scope is not registered.</li>
     * </ul>
     * Both arguments must be non-null; the implementation is expected
     * to reject null inputs (typically via {@code Objects.requireNonNull}).
     *
     * @param qualifier the resolved qualifier symbol; never null
     * @param name      the member name to look up; never null, non-empty
     * @return the member symbol, or empty when not found / unsupported
     */
    Optional<Symbol> lookupMember(Symbol qualifier, String name);
}
