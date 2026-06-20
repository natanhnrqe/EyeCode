package com.eyecode.project;

import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProjectInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String path;
    private final ProjectType type;
    private final long lastOpened;

    public ProjectInfo(String name, String path, ProjectType type, long lastOpened) {
        this.name = name;
        this.path = path;
        this.type = type;
        this.lastOpened = lastOpened;
    }

    public ProjectInfo(String name, String path, ProjectType type) {
        this(name, path, type, System.currentTimeMillis());
    }

    public String getName() { return name; }
    public String getPath() { return path; }
    public ProjectType getType() { return type; }
    public long getLastOpened() { return lastOpened; }

    public Path toPath() { return Paths.get(path); }

    public ProjectInfo withLastOpened(long lastOpened) {
        return new ProjectInfo(name, path, type, lastOpened);
    }

    public ProjectInfo withType(ProjectType type) {
        return new ProjectInfo(name, path, type, lastOpened);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ProjectInfo that)) return false;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public String toString() {
        return name + " [" + type.getDisplayName() + "]";
    }
}
