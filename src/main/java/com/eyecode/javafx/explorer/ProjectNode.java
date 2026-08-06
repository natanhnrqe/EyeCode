package com.eyecode.javafx.explorer;

import java.nio.file.Path;

public record ProjectNode(String name, Path path, ProjectNodeType type) {

    public boolean isDirectory() {
        return type != ProjectNodeType.FILE;
    }
}
