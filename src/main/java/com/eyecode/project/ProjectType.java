package com.eyecode.project;

public enum ProjectType {
    SPRING_BOOT("Spring Boot"),
    MAVEN("Maven"),
    GRADLE("Gradle"),
    GIT("Git Repository"),
    JAVA("Java Project"),
    UNKNOWN("Unknown");

    private final String displayName;

    ProjectType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getIconName() {
        return switch (this) {
            case SPRING_BOOT -> "spring";
            case MAVEN       -> "maven";
            case GRADLE      -> "gradle";
            case GIT         -> "git";
            case JAVA        -> "java";
            case UNKNOWN     -> "projectDirectory";
        };
    }
}
