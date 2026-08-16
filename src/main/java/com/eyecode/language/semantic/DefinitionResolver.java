package com.eyecode.language.semantic;

import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolTable;

import java.util.Optional;

/**
 * Core API for "where is this symbol declared?" (Sprint 5.4c.1).
 * <p>
 * Conceptually the Core analogue of LSP
 * <em>textDocument/definition</em>. Given a {@link SymbolReference}
 * (the textual occurrence at a specific source range) and the
 * {@link SymbolTable} that backs the document, returns the
 * {@link DefinitionLocation} of the symbol's declaration — or empty
 * when the reference cannot be resolved with the current Core
 * infrastructure.
 * <p>
 * <b>Contract</b>:
 * <ul>
 *   <li>never returns a fabricated or speculative location — only
 *       resolved declarations from the supplied
 *       {@link SymbolTable};</li>
 *   <li>unresolvable references yield an empty {@link Optional}
 *       rather than throwing;</li>
 *   <li>{@code null} arguments are rejected via
 *       {@link NullPointerException};</li>
 *   <li>the {@link SymbolTable} is read-only — implementations must
 *       not mutate it.</li>
 * </ul>
 * <p>
 * <b>Out of scope</b> (deferred): overload resolution, inheritance,
 * interface/superclass resolution, generic substitution, type
 * inference, imports (regular / static / on-demand), cross-file
 * resolution, method dispatch, ambiguity detection. References that
 * depend on these layers return {@link Optional#empty()}.
 * <p>
 * <b>Editor integration</b> is out of scope for this sprint — there
 * is no Swing / JavaFX / AWT / editor-ui coupling. A future sprint
 * will wire this API into editor navigation actions.
 */
public interface DefinitionResolver {

    /**
     * Resolves the textual reference to the location of its symbol's
     * declaration.
     *
     * @param reference  the textual occurrence to resolve; never null
     * @param symbolTable the symbol table backing the document; never null
     * @return the definition location when the reference resolves to a
     *         declaration supported by the current Core infrastructure;
     *         empty otherwise
     * @throws NullPointerException if either argument is null
     */
    Optional<DefinitionLocation> resolve(SymbolReference reference, SymbolTable symbolTable);
}
