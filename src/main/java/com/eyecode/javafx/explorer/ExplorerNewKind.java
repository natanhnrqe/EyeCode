package com.eyecode.javafx.explorer;

import com.eyecode.project.ProjectCreationService;

public enum ExplorerNewKind {
    PACKAGE,
    JAVA_CLASS,
    INTERFACE,
    ENUM,
    RECORD,
    MAIN_CLASS,
    JAVA_FILE;

    public ProjectCreationService.JavaTypeKind javaTypeKind() {
        return switch (this) {
            case JAVA_CLASS -> ProjectCreationService.JavaTypeKind.CLASS;
            case INTERFACE -> ProjectCreationService.JavaTypeKind.INTERFACE;
            case ENUM -> ProjectCreationService.JavaTypeKind.ENUM;
            case RECORD -> ProjectCreationService.JavaTypeKind.RECORD;
            case MAIN_CLASS -> ProjectCreationService.JavaTypeKind.MAIN_CLASS;
            default -> null;
        };
    }
}
