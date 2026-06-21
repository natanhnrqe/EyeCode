package com.eyecode.explorer.model;

import com.eyecode.explorer.visitor.ExplorerTreeVisitor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ExplorerTree {

    private final ExplorerNode root;

    public ExplorerTree(ExplorerNode root) {
        this.root = root;
    }

    public ExplorerNode getRoot() {
        return root;
    }

    public List<ExplorerNode> flatten() {
        List<ExplorerNode> nodes = new ArrayList<>();
        collect(root, nodes);
        return List.copyOf(nodes);
    }

    public Optional<ExplorerNode> findByPath(Path path) {
        if (path == null) return Optional.empty();
        Path normalized = path.toAbsolutePath().normalize();
        return flatten().stream()
                .filter(node -> node.getPath().toAbsolutePath().normalize().equals(normalized))
                .findFirst();
    }

    public void traverse(ExplorerTreeVisitor visitor) {
        if (visitor == null) return;
        traverse(root, visitor);
    }

    private void collect(ExplorerNode node, List<ExplorerNode> nodes) {
        nodes.add(node);
        for (ExplorerNode child : node.getChildren()) {
            collect(child, nodes);
        }
    }

    private void traverse(ExplorerNode node, ExplorerTreeVisitor visitor) {
        visitor.visit(node);
        for (ExplorerNode child : node.getChildren()) {
            traverse(child, visitor);
        }
    }
}
