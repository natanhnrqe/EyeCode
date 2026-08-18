package com.eyecode.language.semantic;

import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolModifier;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;

import java.util.Objects;
import java.util.Optional;

/**
 * Default {@link QualifiedMemberLookup} backed by the
 * {@link SymbolTable}'s scope structure (Sprint 5.4b.6).
 * <p>
 * Member lookup is restricted to the {@link SymbolKind}s the current
 * {@link SymbolTable} can represent structurally:
 * <ul>
 *   <li><b>Type-like qualifiers</b> ({@link SymbolKind#TYPE},
 *       {@link SymbolKind#INTERFACE}, {@link SymbolKind#ENUM},
 *       {@link SymbolKind#ANNOTATION}) — the member is searched in the
 *       qualifier's own scope via {@code symbol.scopeId()} (the
 *       {@link SymbolTableBuilder} sets {@code scopeId} to the type's
 *       own scope where its declared fields / methods / nested types
 *       live). Lookup is local — {@code findLocal}, no parent
 *       traversal — so an unqualified member does not leak into
 *       enclosing scopes (spec §12).</li>
 *   <li><b>Package qualifier</b> ({@link SymbolKind#PACKAGE}) — the
 *       member is searched in the package's own scope via
 *       {@code symbol.scopeId()} (top-level types declared in the
 *       package). This implementation only resolves a member when the
 *       table actually registered a PACKAGE scope and that scope
 *       declares a matching symbol; arbitrary package discovery is
 *       <b>not</b> implemented (spec §7 — "não criar um mecanismo
 *       global de package discovery").</li>
 * </ul>
 * <p>
 * All other qualifier kinds (METHOD, CONSTRUCTOR, FIELD, PARAMETER,
 * LOCAL_VARIABLE, TYPE_PARAMETER) yield empty — the symbol carries no
 * member-context information that the current model can interpret
 * without type inference (spec §6). This is intentional: the resolver
 * does NOT fabricate type information for variables.
 * <p>
 * The lookup is read-only: it never mutates the symbol, the scope or
 * the table.
 * <p>
 * Implementation note: a missing member scope (no scope registered
 * under {@code symbol.scopeId()}) is treated as a structural
 * limitation, not an error — the lookup simply yields empty so the
 * resolver can mark the chain as {@code UNRESOLVED}.
 */
public final class ScopeBasedQualifiedMemberLookup implements QualifiedMemberLookup {

    private final SymbolTable table;

    /**
     * Creates a new lookup bound to the given {@link SymbolTable}.
     *
     * @param table the symbol table to consult; never null
     */
    public ScopeBasedQualifiedMemberLookup(SymbolTable table) {
        this.table = Objects.requireNonNull(table, "table must not be null");
    }

    @Override
    public Optional<Symbol> lookupMember(Symbol qualifier, String name) {
        return lookupMember(qualifier, name, QualifiedMemberExpectation.ANY);
    }

    @Override
    public Optional<Symbol> lookupMember(Symbol qualifier,
                                         String name,
                                         QualifiedMemberExpectation expectation) {
        Objects.requireNonNull(qualifier, "qualifier must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(expectation, "expectation must not be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (!supportsMemberLookup(qualifier.kind())) {
            return Optional.empty();
        }
        Optional<SymbolScope> memberScope = table.scope(qualifier.scopeId());
        if (memberScope.isEmpty()) {
            return Optional.empty();
        }
        return memberScope.get().findLocal(name)
                .filter(member -> matches(member, qualifier.kind(), expectation));
    }

    private static boolean matches(Symbol member,
                                   SymbolKind qualifierKind,
                                   QualifiedMemberExpectation expectation) {
        return switch (expectation) {
            case ANY -> true;
            case STATIC_FIELD -> member.kind() == SymbolKind.FIELD
                    && (member.modifiers().contains(SymbolModifier.STATIC)
                    || qualifierKind == SymbolKind.INTERFACE);
            case STATIC_METHOD -> member.kind() == SymbolKind.METHOD
                    && member.modifiers().contains(SymbolModifier.STATIC);
            case CONSTRUCTOR -> member.kind() == SymbolKind.CONSTRUCTOR;
        };
    }

    /**
     * Whether this implementation supports member lookup for the given
     * qualifier kind under the current {@link SymbolTable} model.
     *
     * @param kind the qualifier kind; never null
     * @return {@code true} iff the kind is one of {@link SymbolKind#TYPE},
     *         {@link SymbolKind#INTERFACE}, {@link SymbolKind#ENUM},
     *         {@link SymbolKind#ANNOTATION} or {@link SymbolKind#PACKAGE}
     */
    public static boolean supportsMemberLookup(SymbolKind kind) {
        Objects.requireNonNull(kind, "kind must not be null");
        return switch (kind) {
            case TYPE, INTERFACE, ENUM, ANNOTATION, PACKAGE -> true;
            default -> false;
        };
    }
}
