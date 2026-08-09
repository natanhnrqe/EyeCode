package com.eyecode.language.ast;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Utilities for the immutable AST: the post-parse parent linking pass and
 * pre-order traversal.
 */
public final class AstNodes {

    private AstNodes() {
    }

    /**
     * Post-parse linking pass: assigns every node its parent and guarantees
     * the bidirectional invariant {@code child.parent() == parent} and
     * {@code parent.children().contains(child)} for the whole subtree.
     *
     * @return the root, with {@code parent() == null}
     */
    public static AstNode linkParents(AstNode root) {
        if (root == null) {
            throw new IllegalArgumentException("root must not be null");
        }
        linkParents(root, null);
        return root;
    }

    private static void linkParents(AstNode node, AstNode parent) {
        if (!(node instanceof AstNode.AstNodeImpl impl)) {
            throw new IllegalStateException(
                    "unsupported AstNode implementation: " + node.getClass().getName());
        }
        impl.link(parent);
        for (AstNode child : node.children()) {
            linkParents(child, node);
        }
    }

    /**
     * Pre-order traversal: the visitor is called on a node before its
     * children.
     */
    public static void traverse(AstNode root, AstVisitor visitor) {
        if (root == null || visitor == null) {
            throw new IllegalArgumentException("root and visitor must not be null");
        }
        Deque<AstNode> stack = new ArrayDeque<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            AstNode node = stack.pop();
            visitor.visit(node);
            List<AstNode> children = node.children();
            for (int i = children.size() - 1; i >= 0; i--) {
                stack.push(children.get(i));
            }
        }
    }

    /**
     * All descendants of {@code root} (excluding the root), pre-order.
     */
    public static List<AstNode> descendants(AstNode root) {
        List<AstNode> result = new ArrayList<>();
        traverse(root, new AstVisitor() {
            @Override
            public void visit(AstNode node) {
                if (node != root) {
                    result.add(node);
                }
            }
        });
        return result;
    }
}
