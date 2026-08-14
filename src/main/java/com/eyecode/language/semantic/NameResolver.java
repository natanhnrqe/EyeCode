package com.eyecode.language.semantic;

import com.eyecode.language.java.parser.ParserSnapshot;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolTable;

import java.util.List;

/**
 * Interface for name resolution services (Sprint 5.4b.1 — batch contract,
 * expanded in 5.4b.2 to a single-reference contract).
 * <p>
 * A name resolver takes a parsed AST (via {@link ParserSnapshot}) and a
 * symbol table, and produces a list of resolved symbol references. The
 * single-reference form {@link #resolve(SymbolReference, SymbolTable)}
 * resolves one reference at a time using the {@link SymbolReference#name()}
 * and {@link SymbolReference#scopeId()} held by the reference itself.
 * <p>
 * The resolver does not modify the AST, the symbol table, the symbol
 * scopes, or the symbol references — it only produces resolution results.
 */
public interface NameResolver {

    /**
     * Resolves a single simple-name reference against a symbol table using
     * a hierarchical lookup starting at {@link SymbolReference#scopeId()}.
     * <p>
     * Result kinds:
     * <ul>
     *   <li>{@link ResolutionKind#RESOLVED} when exactly one applicable
     *       symbol is found by walking the scope chain from the reference's
     *       scope toward the root.</li>
     *   <li>{@link ResolutionKind#UNRESOLVED} when no symbol matches the name.</li>
     *   <li>{@link ResolutionKind#AMBIGUOUS} only when the underlying scope
     *       model genuinely produces more than one valid resolution; the
     *       current {@code SymbolScope} does not, so this kind is reserved
     *       for future extensions (overload / cross-import resolution).</li>
     * </ul>
     * No artificial ambiguity is ever fabricated.
     *
     * @param reference   the simple-name reference to resolve (never {@code null})
     * @param symbolTable the symbol table to resolve against (never {@code null})
     * @return the resolution outcome; never {@code null}
     */
    ResolvedSymbolReference resolve(SymbolReference reference, SymbolTable symbolTable);

    /**
     * Resolves all simple-name references in the given parser snapshot
     * against the provided symbol table. Implementations discover
     * references by walking the AST and use
     * {@link #resolve(SymbolReference, SymbolTable)} for each individual
     * reference so the hierarchical lookup with shadowing is shared
     * between both entry points.
     *
     * @param parserSnapshot the parser snapshot containing the AST to resolve
     * @param symbolTable    the symbol table containing declarations
     * @return immutable list of resolved symbol references,
     *         one per simple-name reference discovered in the AST
     */
    List<ResolvedSymbolReference> resolve(ParserSnapshot parserSnapshot, SymbolTable symbolTable);
}
