package com.eyecode.eventbus.events;

import com.eyecode.eventbus.Event;

import java.nio.file.Path;

/**
 * Carries information about an incremental change to the project file system.
 * <p>
 * The explorer and the symbol index subscribe to this event to update only the
 * affected node or symbols without rebuilding the whole tree.
 */
public final class ProjectRefreshEvent implements Event {

    public enum Kind {
        FILE_CREATED,
        FILE_DELETED,
        FILE_RENAMED,
        FILE_MODIFIED,
        DIRECTORY_CREATED,
        DIRECTORY_DELETED
    }

    private final Kind kind;
    private final Path path;
    private final Path oldPath;

    public ProjectRefreshEvent(Kind kind, Path path) {
        this(kind, path, null);
    }

    public ProjectRefreshEvent(Kind kind, Path path, Path oldPath) {
        this.kind = kind;
        this.path = path;
        this.oldPath = oldPath;
    }

    public Kind getKind() { return kind; }

    public Path getPath() { return path; }

    public Path getOldPath() { return oldPath; }

    public boolean hasOldPath() { return oldPath != null; }
}
