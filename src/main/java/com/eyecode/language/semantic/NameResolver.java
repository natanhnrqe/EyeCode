package com.eyecode.language.semantic;

import com.eyecode.language.java.parser.ParserSnapshot;
import com.eyecode.language.symbol.SymbolTable;

import java.util.List;

/**
 * Interface for name resolution services (Sprint 5.4b.1).
 * <p>
 * A name resolver takes a parsed AST (via {@link ParserSnapshot}) and a
 * symbol table, and produces a list of resolved symbol references.
 * <p>
 * The resolver does not modify the AST or the symbol table — it only
 * produces resolution results.
 */
public interface NameResolver {

    /**
     * Resolves all name references in the given parser snapshot against
     * the provided symbol table.
     *
     * @param parserSnapshot the parser snapshot containing the AST to resolve
     * @param symbolTable the symbol table containing declarations
     * @return list of resolved symbol references, one per name reference in the AST
     */
    List<ResolvedSymbolReference> resolve(ParserSnapshot parserSnapshot, SymbolTable symbolTable);
}