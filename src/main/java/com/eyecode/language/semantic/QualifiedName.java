package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Immutable ordered decomposition of a dot-separated qualified name
 * (Sprint 5.4b.5).
 * <p>
 * A {@link QualifiedName} holds the ordered list of
 * {@link QualifiedNameComponent components} that make up the reference.
 * The decomposition is <em>purely syntactic</em> — it does not consult the
 * {@code SymbolTable}, any {@code SymbolScope}, the parser, imports or
 * types. The meaning of each component (qualifier type, field, package,
 * type, etc.) is left to later sprints.
 * <p>
 * For {@code foo.bar.baz} the components are, in source order:
 * <ol>
 *   <li>{@code foo [0,3)}</li>
 *   <li>{@code bar [4,7)}</li>
 *   <li>{@code baz [8,11)}</li>
 * </ol>
 * <p>
 * <b>Construction rules</b> (enforced by {@link #of}):
 * <ul>
 *   <li>the component list is non-null and non-empty</li>
 *   <li>a {@link QualifiedName} must contain <em>at least two</em>
 *       components — a single-identifier reference is not a qualified name</li>
 *   <li>components are stored in a defensive copy and exposed as an
 *       unmodifiable list</li>
 * </ul>
 * <p>
 * The convenience accessors {@link #qualifier()} and {@link #terminalName()}
 * are provided for ergonomic use but are computed from the component list
 * (no extra objects are retained): {@code qualifier()} is the leftmost
 * component, {@code terminalName()} is the rightmost. The {@link #range()}
 * accessor returns the {@link TextRange} spanning from the first
 * component's start to the last component's end.
 * <p>
 * Immutable and thread-safe. Equality is by value (component list).
 */
public final class QualifiedName {

    private final List<QualifiedNameComponent> components;

    private QualifiedName(List<QualifiedNameComponent> components) {
        this.components = List.copyOf(components);
    }

    /**
     * Constructs a {@link QualifiedName} from an ordered list of components.
     *
     * @param components the ordered components, non-null, at least 2 entries
     * @return a new immutable {@link QualifiedName}
     * @throws NullPointerException     if {@code components} or any entry is null
     * @throws IllegalArgumentException if the list has fewer than 2 components
     */
    public static QualifiedName of(List<QualifiedNameComponent> components) {
        Objects.requireNonNull(components, "components must not be null");
        if (components.size() < 2) {
            throw new IllegalArgumentException(
                    "a qualified name requires at least 2 components, got " + components.size());
        }
        for (QualifiedNameComponent c : components) {
            Objects.requireNonNull(c, "component must not be null");
        }
        return new QualifiedName(components);
    }

    /**
     * Number of components in this qualified name.
     *
     * @return the component count; always {@code >= 2}
     */
    public int componentCount() {
        return components.size();
    }

    /**
     * Returns the component at the given index.
     *
     * @param index 0-based component index, in {@code [0, componentCount())}
     * @return the component at that index; never null
     * @throws IndexOutOfBoundsException if the index is out of range
     */
    public QualifiedNameComponent component(int index) {
        return components.get(index);
    }

    /**
     * The ordered list of components.
     *
     * @return an unmodifiable view of the component list; never null, never empty
     */
    public List<QualifiedNameComponent> components() {
        return components;
    }

    /**
     * The first (leftmost) component of this qualified name.
     * <p>
     * For {@code foo.bar.baz} this returns the {@code foo} component.
     *
     * @return the qualifier component; never null
     */
    public QualifiedNameComponent qualifier() {
        return components.get(0);
    }

    /**
     * The last (rightmost) component of this qualified name.
     * <p>
     * For {@code foo.bar.baz} this returns the {@code baz} component.
     *
     * @return the terminal-name component; never null
     */
    public QualifiedNameComponent terminalName() {
        return components.get(components.size() - 1);
    }

    /**
     * The source range spanning from the start of the first component to
     * the end of the last component.
     * <p>
     * For {@code foo.bar.baz} starting at offset 0 the range is
     * {@code [0, 11)} — the dots are included in the span.
     *
     * @return the full range of this qualified name; never null
     */
    public TextRange range() {
        int start = components.get(0).range().startOffset();
        int end = components.get(components.size() - 1).range().endOffset();
        return TextRange.of(start, end);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QualifiedName that)) return false;
        return components.equals(that.components);
    }

    @Override
    public int hashCode() {
        return components.hashCode();
    }

    @Override
    public String toString() {
        List<String> names = new ArrayList<>(components.size());
        for (QualifiedNameComponent c : components) {
            names.add(c.name());
        }
        return "QualifiedName[" + String.join(".", names) + "]";
    }
}
