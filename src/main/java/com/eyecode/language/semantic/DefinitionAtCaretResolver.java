package com.eyecode.language.semantic;

import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolTable;

import java.util.Objects;
import java.util.Optional;

/**
 * Caret-driven facade for "Go to Definition" (Sprint 5.4c.2).
 * <p>
 * End-to-end pipeline:
 * <pre>
 *   caret offset
 *     ↓
 *   DefinitionReferenceResolver
 *     ↓
 *   Optional<SymbolReference>
 *     ↓
 *   DefinitionResolver
 *     ↓
 *   Optional<DefinitionLocation>
 * </pre>
 * <p>
 * <b>Contract</b>:
 * <ul>
 *   <li>empty when the caret does not sit on an identifier (or
 *       contiguous qualified name) — no reference exists;</li>
 *   <li>empty when the reference exists but the symbol cannot be
 *       resolved with the current Core infrastructure — no
 *       fabricated location;</li>
 *   <li>{@link Optional} carrying a {@link DefinitionLocation} when
 *       the caret sits on a reference that resolves to a
 *       declaration supported by the existing pipelines.</li>
 * </ul>
 * <p>
 * <b>Editor integration</b> is OUT OF SCOPE for this sprint — no
 * Swing / JavaFX / AWT / editor-ui coupling. A future sprint will
 * wire this API into editor navigation actions (Ctrl+Click / F12 /
 * Ctrl+B). This class is the last Core-only step on the path.
 * <p>
 * Pure Core: zero Swing / JavaFX / AWT / editor-ui / workbench imports.
 */
public final class DefinitionAtCaretResolver {

    private final DefinitionReferenceResolver referenceResolver;
    private final DefinitionResolver definitionResolver;

    /**
     * Creates a facade with default collaborators
     * ({@link DefinitionReferenceResolver} + {@link JavaDefinitionResolver}).
     */
    public DefinitionAtCaretResolver() {
        this(new DefinitionReferenceResolver(), new JavaDefinitionResolver());
    }

    /**
     * Creates a facade with explicit collaborators (testability /
     * customisation).
     *
     * @param referenceResolver the caret-to-reference pipeline; never null
     * @param definitionResolver the reference-to-location pipeline; never null
     */
    public DefinitionReferenceResolver referenceResolver() {
        return referenceResolver;
    }

    public DefinitionResolver definitionResolver() {
        return definitionResolver;
    }

    public DefinitionAtCaretResolver(DefinitionReferenceResolver referenceResolver,
                                     DefinitionResolver definitionResolver) {
        this.referenceResolver = Objects.requireNonNull(referenceResolver,
                "referenceResolver must not be null");
        this.definitionResolver = Objects.requireNonNull(definitionResolver,
                "definitionResolver must not be null");
    }

    /**
     * Resolves the {@link DefinitionLocation} for the textual
     * reference under the caret.
     *
     * @param source the document source text; never null
     * @param offset the caret offset; in {@code [0, source.length()]}
     * @param table  the current symbol table; never null
     * @return the definition location when both the reference and the
     *         symbol resolve; empty otherwise
     * @throws NullPointerException if any argument is null
     */
    public Optional<DefinitionLocation> resolve(String source, int offset, SymbolTable table) {
        Objects.requireNonNull(source, "source must not be null");
        Objects.requireNonNull(table, "table must not be null");

        Optional<SymbolReference> reference = referenceResolver.resolve(source, offset, table);
        if (reference.isEmpty()) {
            return Optional.empty();
        }
        return definitionResolver.resolve(reference.get(), table);
    }
}
