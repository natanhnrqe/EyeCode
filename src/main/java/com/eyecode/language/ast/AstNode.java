package com.eyecode.language.ast;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.Token;

import java.util.List;
import java.util.Objects;

/**
 * Immutable, navigable node of the declarative AST (Sprint 5.3a).
 * <p>
 * A node carries its absolute {@link TextRange} in the document, its kind and
 * its children. Parent links are NOT established at construction time — they
 * are assigned by the post-parse linking pass ({@link AstNodes#linkParents}),
 * which keeps node creation simple and the tree consistent.
 * <p>
 * No parsing logic lives here; the AST is built by the parser and consumed by
 * future semantic/diagnostic/completion services.
 */
public interface AstNode {

    /**
     * Absolute range of this node in the source document.
     */
    TextRange range();

    /**
     * Parent node, or {@code null} for the root (compilation unit).
     */
    AstNode parent();

    /**
     * Immutable list of child nodes, in source order.
     */
    List<AstNode> children();

    AstNodeKind kind();

    /**
     * Optional lexical token this node was built from (Sprint 5.3c). Set for
     * expression leaves that need to keep their source text or literal value
     * (names, literals, operators, method-reference names); {@code null}
     * otherwise.
     */
    default Token token() {
        return null;
    }

    /**
     * Creates an unlinked node (parent assigned later by
     * {@link AstNodes#linkParents}). The children list is defensively copied.
     */
    static AstNode of(AstNodeKind kind, TextRange range, List<AstNode> children) {
        return new AstNodeImpl(kind, range, children, null);
    }

    /**
     * Creates an unlinked node carrying an optional lexical token (Sprint
     * 5.3c). The children list is defensively copied.
     */
    static AstNode of(AstNodeKind kind, TextRange range, List<AstNode> children, Token token) {
        return new AstNodeImpl(kind, range, children, token);
    }

    final class AstNodeImpl implements AstNode {

        private final AstNodeKind kind;
        private final TextRange range;
        private final List<AstNode> children;
        private final Token token;
        private AstNode parent;

        AstNodeImpl(AstNodeKind kind, TextRange range, List<AstNode> children, Token token) {
            this.kind = Objects.requireNonNull(kind, "kind must not be null");
            this.range = Objects.requireNonNull(range, "range must not be null");
            this.children = List.copyOf(Objects.requireNonNull(children, "children must not be null"));
            this.token = token;
        }

        @Override
        public TextRange range() {
            return range;
        }

        @Override
        public AstNode parent() {
            return parent;
        }

        @Override
        public List<AstNode> children() {
            return children;
        }

        @Override
        public AstNodeKind kind() {
            return kind;
        }

        @Override
        public Token token() {
            return token;
        }

        void link(AstNode parent) {
            if (this.parent != null) {
                throw new IllegalStateException("node already linked");
            }
            this.parent = parent;
        }

        @Override
        public String toString() {
            return "AstNode[" + kind + " " + range + "]";
        }
    }
}
