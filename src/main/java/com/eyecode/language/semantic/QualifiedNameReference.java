package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Objects;

/**
 * Immutable qualified-name reference model (Sprint 5.4b.4).
 * <p>
 * Represents a textual qualified reference such as {@code foo.bar},
 * {@code a.b.c}, {@code this.value} or {@code pkg.Type} by carrying the
 * <em>qualifier</em> (the leftmost component) and the <em>terminal
 * name</em> (the rightmost component), each with its own
 * {@link TextRange}. Intermediate components of a 3+-dot name are not
 * decomposed — that remains for a later sprint.
 * <p>
 * For {@code foo.bar}: qualifier = {@code "foo"}, name = {@code "bar"},
 * with {@code qualifierRange = [0,3)} and {@code nameRange = [4,7)} —
 * the dot occupies offset 3.
 * <p>
 * For a 3+ component name such as {@code foo.bar.baz}: qualifier =
 * the leftmost component ({@code "foo"}), name = the rightmost
 * component ({@code "baz"}); intermediate components are part of the
 * syntactic gap between {@code qualifierRange.endOffset()} and
 * {@code nameRange.startOffset()} and are intentionally not modelled
 * here (5.4b.5+ will extend this).
 * <p>
 * <b>Construction rules</b> (enforced by {@link #of}):
 * <ul>
 *   <li>{@code qualifier} non-null, non-empty</li>
 *   <li>{@code name} non-null, non-empty</li>
 *   <li>{@code qualifierRange}, {@code nameRange} non-null</li>
 *   <li>{@code qualifierRange.length()} === qualifier length</li>
 *   <li>{@code nameRange.length()} === name length</li>
 *   <li>{@code qualifierRange.endOffset() <= nameRange.startOffset()} —
 *       the qualifier precedes the name; dot(s) live in the gap</li>
 * </ul>
 * <p>
 * The instance is immutable and thread-safe. Equality is by value
 * (all four fields).
 */
public final class QualifiedNameReference {

    private final String qualifier;
    private final String name;
    private final TextRange qualifierRange;
    private final TextRange nameRange;

    private QualifiedNameReference(String qualifier, String name,
                                   TextRange qualifierRange, TextRange nameRange) {
        this.qualifier = qualifier;
        this.name = name;
        this.qualifierRange = qualifierRange;
        this.nameRange = nameRange;
    }

    /**
     * Constructs a {@link QualifiedNameReference}.
     *
     * @param qualifier       the qualifier (leftmost component), non-null / non-empty
     * @param name            the terminal name (rightmost component), non-null / non-empty
     * @param qualifierRange  source range of the qualifier, non-null, length matches {@code qualifier.length()}
     * @param nameRange       source range of the terminal name, non-null, length matches {@code name.length()}
     * @return a new immutable {@link QualifiedNameReference}
     * @throws NullPointerException     if any argument is null
     * @throws IllegalArgumentException if a name is empty or ranges disagree with the name length,
     *                                  or if {@code qualifierRange} does not precede {@code nameRange}
     */
    public static QualifiedNameReference of(String qualifier, String name,
                                            TextRange qualifierRange, TextRange nameRange) {
        Objects.requireNonNull(qualifier, "qualifier must not be null");
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(qualifierRange, "qualifierRange must not be null");
        Objects.requireNonNull(nameRange, "nameRange must not be null");
        if (qualifier.isEmpty()) {
            throw new IllegalArgumentException("qualifier must not be empty");
        }
        if (name.isEmpty()) {
            throw new IllegalArgumentException("name must not be empty");
        }
        int qLen = qualifierRange.length();
        if (qLen != qualifier.length()) {
            throw new IllegalArgumentException(
                    "qualifierRange length (" + qLen + ") does not match qualifier length ("
                            + qualifier.length() + ")");
        }
        int nLen = nameRange.length();
        if (nLen != name.length()) {
            throw new IllegalArgumentException(
                    "nameRange length (" + nLen + ") does not match name length ("
                            + name.length() + ")");
        }
        if (qualifierRange.endOffset() > nameRange.startOffset()) {
            throw new IllegalArgumentException(
                    "qualifierRange end (" + qualifierRange.endOffset()
                            + ") must not exceed nameRange start (" + nameRange.startOffset() + ")");
        }
        return new QualifiedNameReference(qualifier, name, qualifierRange, nameRange);
    }

    /**
     * The leftmost component of the qualified name (the qualifier).
     *
     * @return the qualifier; never null or empty
     */
    public String qualifier() {
        return qualifier;
    }

    /**
     * The rightmost component of the qualified name (the terminal name).
     *
     * @return the name; never null or empty
     */
    public String name() {
        return name;
    }

    /**
     * Source range of the qualifier.
     *
     * @return qualifier range; never null
     */
    public TextRange qualifierRange() {
        return qualifierRange;
    }

    /**
     * Source range of the terminal name.
     *
     * @return name range; never null
     */
    public TextRange nameRange() {
        return nameRange;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof QualifiedNameReference that)) return false;
        return qualifier.equals(that.qualifier)
                && name.equals(that.name)
                && qualifierRange.equals(that.qualifierRange)
                && nameRange.equals(that.nameRange);
    }

    @Override
    public int hashCode() {
        return Objects.hash(qualifier, name, qualifierRange, nameRange);
    }

    @Override
    public String toString() {
        return "QualifiedNameReference[qualifier=" + qualifier
                + ", name=" + name
                + ", qRange=" + qualifierRange.startOffset() + ".." + qualifierRange.endOffset()
                + ", nRange=" + nameRange.startOffset() + ".." + nameRange.endOffset()
                + "]";
    }
}
