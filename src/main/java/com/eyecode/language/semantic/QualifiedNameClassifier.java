package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Optional;

/**
 * Purely-syntactic classifier that decides whether a textual reference is
 * a {@link Kind#SIMPLE_NAME SIMPLE_NAME} (single identifier, no dot) or a
 * {@link Kind#QUALIFIED_NAME QUALIFIED_NAME} (at least one dot separating
 * the qualifier from the terminal name).
 * <p>
 * The classification is <em>purely syntactic</em> — it is built only from
 * the textual shape. It does NOT consult the {@code SymbolTable}, any
 * {@code SymbolScope}, a {@code NameResolver}, the semantic AST, or
 * imports.
 * <p>
 * For a {@link Kind#QUALIFIED_NAME} result, a {@link QualifiedNameReference}
 * is built carrying the leftmost component as the qualifier and the
 * rightmost component as the terminal name. Intermediate components of a
 * 3+-dot name (e.g. {@code foo.bar.baz}) are intentionally not modelled —
 * that decomposition belongs to a later sprint.
 * <p>
 * Invalid inputs recognised as {@link Kind#INVALID}:
 * <ul>
 *   <li>{@code null} text</li>
 *   <li>empty string ({@code ""})</li>
 *   <li>string with a leading dot ({@code .bar})</li>
 *   <li>string with a trailing dot ({@code foo.})</li>
 *   <li>string with two consecutive dots ({@code foo..bar})</li>
 *   <li>string that is only a dot ({@code .})</li>
 * </ul>
 * The classifier never throws for these common invalid inputs; it simply
 * returns a {@link Result} with {@link Kind#INVALID}.
 */
public final class QualifiedNameClassifier {

    /**
     * Classification kind produced by {@link #classify}.
     */
    public enum Kind {
        /**
         * A single-component identifier reference such as {@code foo}.
         */
        SIMPLE_NAME,
        /**
         * A qualified reference such as {@code foo.bar} or {@code a.b.c}.
         */
        QUALIFIED_NAME,
        /**
         * The input text is not a valid simple or qualified name
         * (empty, leading/trailing/double dot, only a dot, etc.).
         */
        INVALID
    }

    /**
     * Immutable result of classifying a textual reference.
     * <p>
     * When {@link #kind()} is {@link Kind#QUALIFIED_NAME}, {@link #qualified()}
     * returns a populated {@link Optional} carrying the built
     * {@link QualifiedNameReference}. For {@link Kind#SIMPLE_NAME} and
     * {@link Kind#INVALID}, the optional is empty.
     */
    public record Result(Kind kind, Optional<QualifiedNameReference> qualified) {
        public Result {
            java.util.Objects.requireNonNull(kind, "kind must not be null");
            if (qualified == null) {
                qualified = Optional.empty();
            }
        }

        /**
         * Convenience accessor: is this result a {@link Kind#SIMPLE_NAME}?
         */
        public boolean isSimpleName() {
            return kind == Kind.SIMPLE_NAME;
        }

        /**
         * Convenience accessor: is this result a {@link Kind#QUALIFIED_NAME}?
         */
        public boolean isQualifiedName() {
            return kind == Kind.QUALIFIED_NAME;
        }

        /**
         * Convenience accessor: is this result {@link Kind#INVALID}?
         */
        public boolean isInvalid() {
            return kind == Kind.INVALID;
        }
    }

    private QualifiedNameClassifier() {
        // Utility class — only static factory entry points.
    }

    /**
     * Classifies a textual reference starting at the given offset.
     *
     * @param text       the textual reference (e.g. {@code "foo"}, {@code "foo.bar"});
     *                   may be {@code null} for convenience (yields {@link Kind#INVALID})
     * @param baseOffset the offset in the source document where this text starts.
     *                   Must be {@code >= 0}; used to compute absolute ranges.
     * @return the classification result; never {@code null}
     * @throws IllegalArgumentException if {@code baseOffset < 0}
     */
    public static Result classify(String text, int baseOffset) {
        if (baseOffset < 0) {
            throw new IllegalArgumentException("baseOffset < 0: " + baseOffset);
        }
        if (text == null || text.isEmpty()) {
            return new Result(Kind.INVALID, Optional.empty());
        }
        // Reject double dots and a leading or trailing dot cheaply.
        if (text.contains("..")) {
            return new Result(Kind.INVALID, Optional.empty());
        }
        if (text.charAt(0) == '.' || text.charAt(text.length() - 1) == '.') {
            return new Result(Kind.INVALID, Optional.empty());
        }

        int lastDot = text.lastIndexOf('.');
        if (lastDot < 0) {
            // No dot — single-component identifier.
            return new Result(Kind.SIMPLE_NAME, Optional.empty());
        }
        int firstDot = text.indexOf('.');
        String qualifier = text.substring(0, firstDot);
        String terminalName = text.substring(lastDot + 1);
        if (qualifier.isEmpty() || terminalName.isEmpty()) {
            // Defensive — already rejected by leading/trailing-dot checks above.
            return new Result(Kind.INVALID, Optional.empty());
        }
        TextRange qRange = TextRange.of(baseOffset, baseOffset + qualifier.length());
        TextRange nRange = TextRange.of(
                baseOffset + text.length() - terminalName.length(),
                baseOffset + text.length());
        try {
            QualifiedNameReference ref =
                    QualifiedNameReference.of(qualifier, terminalName, qRange, nRange);
            return new Result(Kind.QUALIFIED_NAME, Optional.of(ref));
        } catch (IllegalArgumentException ex) {
            return new Result(Kind.INVALID, Optional.empty());
        }
    }

    /**
     * Equivalent to {@code classify(text, 0)} — use when the text is detached
     * from any source and ranges are computed relative to zero.
     *
     * @param text the textual reference
     * @return the classification result; never {@code null}
     */
    public static Result classify(String text) {
        return classify(text, 0);
    }
}
