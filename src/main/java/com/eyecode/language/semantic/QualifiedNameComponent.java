package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Objects;

/**
 * Immutable single component of a {@link QualifiedName} (Sprint 5.4b.5).
 * <p>
 * A component is one identifier piece of a dot-separated qualified name,
 * together with its {@link TextRange} inside the source text. For
 * {@code foo.bar.baz} the components are
 * {@code foo [0,3)}, {@code bar [4,7)}, {@code baz [8,11)}.
 * <p>
 * Components preserve source order and have no semantic meaning on their
 * own — the decomposition layer (5.4b.5) intentionally leaves symbol
 * resolution to later sprints.
 * <p>
 * <b>Construction rules</b> (enforced by {@link #of}):
 * <ul>
 *   <li>{@code name} non-null and non-empty</li>
 *   <li>{@code range} non-null</li>
 *   <li>{@code range.length() == name.length()} — the range covers exactly
 *       the name characters</li>
 * </ul>
 * <p>
 * Immutable and thread-safe. Equality is by value (both fields).
 */
public final class QualifiedNameComponent {

    private final String name;
    private final TextRange range;

    private QualifiedNameComponent(String name, TextRange range) {
        this.name = name;
        this.range = range;
    }

    /**
     * Constructs a {@link QualifiedNameComponent}.
     *
     * @param name  the component identifier, non-null and non-empty
     * @param range source range of the component, non-null, length must match {@code name.length()}
     * @return a new immutable {@link QualifiedNameComponent}
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if the name is empty or the range length
     *                                  does not match the name length
     */
    public static QualifiedNameComponent of(String name, TextRange range) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(range, "range must not be null");
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        if (range.length() != name.length()) {
            throw new IllegalArgumentException(
                    "range length (" + range.length() + ") does not match name length ("
                            + name.length() + ")");
        }
        return new QualifiedNameComponent(name, range);
    }

    /**
     * The identifier text of this component.
     *
     * @return the component name; never null or empty
     */
    public String name() {
        return name;
    }

    /**
     * Source range of this component in the original text.
     *
     * @return the range; never null
     */
    public TextRange range() {
        return range;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QualifiedNameComponent that)) return false;
        return name.equals(that.name) && range.equals(that.range);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, range);
    }

    @Override
    public String toString() {
        return "QualifiedNameComponent[name=" + name
                + ", range=" + range.startOffset() + ".." + range.endOffset() + "]";
    }
}
