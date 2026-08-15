package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Decomposes a dot-separated textual qualified name into its
 * {@link QualifiedNameComponent components} (Sprint 5.4b.5).
 * <p>
 * The decomposition is <em>purely syntactic</em> — it builds the ordered
 * component list with each component's {@link TextRange}; it does NOT
 * consult the {@code SymbolTable}, any {@code SymbolScope}, the parser,
 * imports or types.
 * <p>
 * Examples (with {@code baseOffset = 0}):
 * <ul>
 *   <li>{@code "foo.bar"} → {@code [foo [0,3), bar [4,7)]}</li>
 *   <li>{@code "foo.bar.baz"} → {@code [foo [0,3), bar [4,7), baz [8,11)]}</li>
 *   <li>{@code "a.b.c.d"} → {@code [a [0,1), b [2,3), c [4,5), d [6,7)]}</li>
 * </ul>
 * <p>
 * With a non-zero {@code baseOffset} all component ranges are shifted by that
 * offset. For example {@code decompose("foo.bar", 100)} produces
 * {@code foo [100,103), bar [104,107)}.
 * <p>
 * <b>Invalid inputs</b> yield an empty {@link Optional} (never throw):
 * <ul>
 *   <li>{@code null} or empty ({@code ""}) text</li>
 *   <li>text with no dot is <em>not</em> a qualified name (single identifier)
 *       → empty Optional</li>
 *   <li>leading dot ({@code ".bar"}), trailing dot ({@code "foo."}),
 *       double dot ({@code "foo..bar"}), triple dot
 *       ({@code "foo...bar"}) — empty components are rejected</li>
 *   <li>any whitespace character anywhere in the text (space, tab, newline,
 *       CR) — the decomposer assumes compact dot-separated names</li>
 *   <li>only-dot text ({@code "."})</li>
 * </ul>
 * <p>
 * This sprint does not perform full Java-identifier grammar validation;
 * that is the lexer's responsibility. The decomposer only rejects empty
 * components and obvious whitespace-based corruption.
 */
public final class QualifiedNameDecomposer {

    private QualifiedNameDecomposer() {
        // Utility class — only static entry points.
    }

    /**
     * Decomposes a qualified name starting at offset 0.
     *
     * @param text the textual reference (e.g. {@code "foo.bar"})
     * @return an {@link Optional} holding the {@link QualifiedName}, or empty
     *         if the input is not a valid qualified name (null, empty, single
     *         identifier, leading/trailing/double dot, whitespace)
     */
    public static Optional<QualifiedName> decompose(String text) {
        return decompose(text, 0);
    }

    /**
     * Decomposes a qualified name starting at the given source offset.
     *
     * @param text       the textual reference; may be {@code null} (yields an
     *                   empty Optional)
     * @param baseOffset the offset in the source document where this text
     *                   starts; all component ranges are shifted by it.
     *                   Must be {@code >= 0}
     * @return an {@link Optional} holding the {@link QualifiedName}, or empty
     *         if the input is not a valid qualified name
     * @throws IllegalArgumentException if {@code baseOffset < 0}
     */
    public static Optional<QualifiedName> decompose(String text, int baseOffset) {
        if (baseOffset < 0) {
            throw new IllegalArgumentException("baseOffset < 0: " + baseOffset);
        }
        if (text == null || text.isEmpty()) {
            return Optional.empty();
        }
        // Any whitespace invalidates the qualified name — the decomposer
        // assumes compact dot-separated text.
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Character.isWhitespace(c)) {
                return Optional.empty();
            }
        }
        if (text.indexOf('.') < 0) {
            // No dot — single identifier, not a qualified name.
            return Optional.empty();
        }
        // Reject leading/trailing/double-dot cheaply.
        if (text.charAt(0) == '.' || text.charAt(text.length() - 1) == '.') {
            return Optional.empty();
        }
        if (text.contains("..")) {
            return Optional.empty();
        }

        List<QualifiedNameComponent> components = new ArrayList<>();
        int start = 0;
        int len = text.length();
        for (int i = 0; i <= len; i++) {
            if (i == len || text.charAt(i) == '.') {
                String name = text.substring(start, i);
                TextRange range = TextRange.of(baseOffset + start, baseOffset + i);
                try {
                    components.add(QualifiedNameComponent.of(name, range));
                } catch (IllegalArgumentException ex) {
                    return Optional.empty();
                }
                start = i + 1;
            }
        }
        try {
            return Optional.of(QualifiedName.of(components));
        } catch (IllegalArgumentException ex) {
            return Optional.empty();
        }
    }
}
