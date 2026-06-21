package com.eyecode.explorer.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class ExplorerNode {

    private final String name;
    private final Path path;
    private final ExplorerNodeType type;
    private ExplorerNode parent;
    private final List<ExplorerNode> children;

    public ExplorerNode(String name, Path path, ExplorerNodeType type) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.children = new ArrayList<>();
    }

    public void addChild(ExplorerNode child) {
        if (child == null) return;
        child.parent = this;
        children.add(child);
    }

    public void removeChild(ExplorerNode child) {
        if (child == null) return;
        if (children.remove(child)) {
            child.parent = null;
        }
    }

    public List<ExplorerNode> getChildren() {
        return Collections.unmodifiableList(children);
    }

    public boolean isLeaf() {
        return children.isEmpty();
    }

    public boolean isRoot() {
        return parent == null;
    }

    public Optional<ExplorerNode> getParent() {
        return Optional.ofNullable(parent);
    }

    public String getName() { return name; }

    public Path getPath() { return path; }

    public ExplorerNodeType getType() { return type; }
}
