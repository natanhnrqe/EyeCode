package com.eyecode.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.ast.AstNode;

import java.util.Objects;

/**
 * Immutable, versioned result of a syntactic analysis.
 * <p>
 * A snapshot is bound to the exact {@link #version()} of the document it was
 * produced from and never changes after creation: the version cannot be
 * altered, the AST root and text are exposed read-only. A stale snapshot
 * stays valid for the version it describes — it is never silently updated
 * when the document advances.
 * <p>
 * The contract is the mirror image of {@link DocumentSnapshot} — the
 * parser observes the document and produces a paired snapshot of the same
 * version.
 *
 * <h2>Equality</h2>
 * Two {@code ParserSnapshot}s are equal when they describe the same
 * document version and parse to structurally equivalent ASTs (see
 * {@link com.eyecode.language.java.parser.incremental.AstEquivalence}).
 * Text identity is also required so a parser produced from an out-of-band
 * snapshot is correctly distinguished from one built by the canonical
 * pipeline.
 */
public final class ParserSnapshot {

    private final long version;
    private final String text;
    private final AstNode astRoot;

    public ParserSnapshot(long version, String text, AstNode astRoot) {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
        if (astRoot == null) {
            throw new IllegalArgumentException("astRoot must not be null");
        }
        this.version = version;
        this.text = text;
        this.astRoot = astRoot;
    }

    /**
     * Convenience factory: builds a snapshot from a {@link DocumentSnapshot}
     * and the AST root produced by parsing it.
     */
    public static ParserSnapshot of(DocumentSnapshot document, AstNode astRoot) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        return new ParserSnapshot(document.version(), document.getText(), astRoot);
    }

    public long version() {
        return version;
    }

    public String text() {
        return text;
    }

    public AstNode astRoot() {
        return astRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParserSnapshot that)) return false;
        return version == that.version
                && text.equals(that.text)
                && astRootEquals(astRoot, that.astRoot);
    }

    private static boolean astRootEquals(AstNode a, AstNode b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return com.eyecode.language.java.parser.incremental.AstEquivalence.equals(a, b);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, text, astRoot.kind(), astRoot.range());
    }

    @Override
    public String toString() {
        return "ParserSnapshot{version=" + version + ", ast=" + astRoot.kind() + "}";
    }
}
