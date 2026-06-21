package com.eyecode.project.model;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProjectModule {

    private final String name;
    private final Path root;
    private final ModuleType type;
    private final List<Path> sourceRoots;

    public ProjectModule(String name, Path root, ModuleType type, List<Path> sourceRoots) {
        this.name = name;
        this.root = root;
        this.type = type;
        this.sourceRoots = Collections.unmodifiableList(new ArrayList<>(sourceRoots));
    }

    public String getName() { return name; }
    public Path getRoot() { return root; }
    public ModuleType getType() { return type; }
    public List<Path> getSourceRoots() { return sourceRoots; }
}
