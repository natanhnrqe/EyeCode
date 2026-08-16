package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.symbol.Symbol;

import java.util.Objects;

/**
 * Immutable description of where a symbol is declared (Sprint 5.4c.1).
 * <p>
 * Carries the resolved {@link Symbol} and the source range of its
 * declaration (the same range used to derive the symbol's
 * {@link com.eyecode.language.symbol.SymbolId}). This is the Core
 * analogue of an LSP {@code Location} for the
 * <em>textual definition</em> of a symbol — it deliberately does NOT
 * carry filesystem path or URI metadata because the current
 * {@link com.eyecode.language.symbol.Symbol} model does not store
 * that information. A future sprint can extend this type (or add a
 * sibling) when source-file metadata becomes available.
 * <p>
 * <b>Rules</b>:
 * <ul>
 *   <li>{@code symbol} is non-null; rejected via {@link NullPointerException}.</li>
 *   <li>{@code declarationRange} is non-null; rejected via
 *       {@link NullPointerException}. The factory
 *       {@link #of(Symbol)} derives the range from
 *       {@code symbol.declarationRange()} so callers normally do not
 *       supply it directly.</li>
 *   <li>Immutable and thread-safe; equality is by value over both
 *       fields.</li>
 *   <li>Pure Core: no Swing / JavaFX / AWT / editor-ui / workbench
 *       imports.</li>
 * </ul>
 */
public final class DefinitionLocation {

    private final Symbol symbol;
    private final TextRange declarationRange;

    /**
     * Builds a {@link DefinitionLocation} by extracting the declaration
     * range from the supplied symbol.
     *
     * @param symbol the resolved symbol; never null
     * @return a {@link DefinitionLocation} pointing to
     *         {@code symbol.declarationRange()}
     * @throws NullPointerException if {@code symbol} is null
     */
    public static DefinitionLocation of(Symbol symbol) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        return new DefinitionLocation(symbol, symbol.declarationRange());
    }

    /**
     * Builds a {@link DefinitionLocation} from explicit components.
     * Validates that {@code declarationRange} matches
     * {@code symbol.declarationRange()} so the two fields never drift.
     *
     * @param symbol           the resolved symbol; never null
     * @param declarationRange the declaration range; must equal
     *                         {@code symbol.declarationRange()}; never null
     * @return a {@link DefinitionLocation}
     * @throws NullPointerException     if either argument is null
     * @throws IllegalArgumentException if {@code declarationRange} differs
     *                                  from {@code symbol.declarationRange()}
     */
    public static DefinitionLocation of(Symbol symbol, TextRange declarationRange) {
        Objects.requireNonNull(symbol, "symbol must not be null");
        Objects.requireNonNull(declarationRange, "declarationRange must not be null");
        TextRange expected = symbol.declarationRange();
        if (!expected.equals(declarationRange)) {
            throw new IllegalArgumentException(
                    "declarationRange " + declarationRange
                            + " must equal symbol.declarationRange() " + expected);
        }
        return new DefinitionLocation(symbol, declarationRange);
    }

    private DefinitionLocation(Symbol symbol, TextRange declarationRange) {
        this.symbol = symbol;
        this.declarationRange = declarationRange;
    }

    /**
     * The resolved {@link Symbol}.
     *
     * @return the symbol; never null
     */
    public Symbol symbol() {
        return symbol;
    }

    /**
     * The source range of the symbol's declaration.
     *
     * @return the declaration range; never null
     */
    public TextRange declarationRange() {
        return declarationRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DefinitionLocation that)) return false;
        return symbol.equals(that.symbol)
                && declarationRange.equals(that.declarationRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(symbol, declarationRange);
    }

    @Override
    public String toString() {
        return "DefinitionLocation[" + symbol.name()
                + " @ " + declarationRange.startOffset()
                + ".." + declarationRange.endOffset() + "]";
    }
}
