package com.eyecode.language.java.parser.incremental;

import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.ast.AstNodes;
import com.eyecode.language.Token;

/**
 * Structural equality for {@link AstNode} trees.
 * <p>
 * Two trees are considered equivalent when they have the same shape:
 * identical kinds, identical ranges, identical children in order, and
 * identical token text where a node carries one. Object identity is NOT
 * used — the incremental parser reuses objects from previous parses while
 * the full parser builds fresh ones; structural equality is the only
 * correct comparison.
 * <p>
 * The comparator ignores parent links (a node's parent is established by
 * the post-parse linking pass and may have been rebuilt). Range equality
 * is exact — every offset must match — so two trees that represent the
 * same source text at the same positions compare equal.
 */
public final class AstEquivalence {

    private AstEquivalence() {
    }

    public static boolean equals(AstNode a, AstNode b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        if (a.kind() != b.kind()) return false;
        if (!a.range().equals(b.range())) return false;
        if (!tokenEquals(a.token(), b.token())) return false;
        var ca = a.children();
        var cb = b.children();
        if (ca.size() != cb.size()) return false;
        for (int i = 0; i < ca.size(); i++) {
            if (!equals(ca.get(i), cb.get(i))) return false;
        }
        return true;
    }

    /**
     * Recursive equivalence including parent links — both trees must have
     * identical structural shape and consistent parent pointers (children's
     * parent must be the corresponding parent node).
     */
    public static boolean equalsDeep(AstNode a, AstNode b) {
        if (!equals(a, b)) return false;
        if (a == null) return true;
        var ca = a.children();
        var cb = b.children();
        for (int i = 0; i < ca.size(); i++) {
            AstNode childA = ca.get(i);
            AstNode childB = cb.get(i);
            if (childA.parent() != a || childB.parent() != b) return false;
            if (!equalsDeep(childA, childB)) return false;
        }
        return true;
    }

    /**
     * Counts the number of nodes in the given subtree (pre-order).
     */
    public static int nodeCount(AstNode root) {
        if (root == null) return 0;
        int[] count = {0};
        AstNodes.traverse(root, new com.eyecode.language.ast.AstVisitor() {
            @Override
            public void visit(AstNode node) {
                count[0]++;
            }
        });
        return count[0];
    }

    private static boolean tokenEquals(Token a, Token b) {
        if (a == b) return true;
        if (a == null || b == null) return false;
        return a.text().equals(b.text());
    }

    /**
     * Convenience: true when the node's kind matches any of the given kinds.
     */
    public static boolean kindIs(AstNode node, AstNodeKind... kinds) {
        if (node == null) return false;
        AstNodeKind actual = node.kind();
        for (AstNodeKind kind : kinds) {
            if (actual == kind) return true;
        }
        return false;
    }
}
