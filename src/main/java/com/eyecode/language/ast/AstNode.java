package com.eyecode.language.ast;

import com.eyecode.editor.intelligence.document.TextRange;

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
     * Creates an unlinked node (parent assigned later by
     * {@link AstNodes#linkParents}). The children list is defensively copied.
     */
    static AstNode of(AstNodeKind kind, TextRange range, List<AstNode> children) {
        return new AstNodeImpl(kind, range, children);
    }

    final class AstNodeImpl implements AstNode {

        private final AstNodeKind kind;
        private final TextRange range;
        private final List<AstNode> children;
        private AstNode parent;

        AstNodeImpl(AstNodeKind kind, TextRange range, List<AstNode> children) {
            this.kind = Objects.requireNonNull(kind, "kind must not be null");
            this.range = Objects.requireNonNull(range, "range must not be null");
            this.children = List.copyOf(Objects.requireNonNull(children, "children must not be null"));
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
