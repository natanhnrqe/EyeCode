package com.eyecode.project.model;

public final class TypeMapper {

    private TypeMapper() {}

    public static ProjectType toProjectType(com.eyecode.project.ProjectType old) {
        if (old == null) return ProjectType.JAVA;
        return switch (old) {
            case SPRING_BOOT -> ProjectType.SPRING;
            case MAVEN       -> ProjectType.MAVEN;
            case GRADLE      -> ProjectType.GRADLE;
            default          -> ProjectType.JAVA;
        };
    }

    public static BuildSystem toBuildSystem(com.eyecode.project.ProjectType old) {
        if (old == null) return BuildSystem.NONE;
        return switch (old) {
            case MAVEN, SPRING_BOOT -> BuildSystem.MAVEN;
            case GRADLE             -> BuildSystem.GRADLE;
            default                 -> BuildSystem.NONE;
        };
    }

    public static com.eyecode.project.ProjectType toOldProjectType(ProjectType t) {
        if (t == null) return com.eyecode.project.ProjectType.UNKNOWN;
        return switch (t) {
            case SPRING -> com.eyecode.project.ProjectType.SPRING_BOOT;
            case MAVEN  -> com.eyecode.project.ProjectType.MAVEN;
            case GRADLE -> com.eyecode.project.ProjectType.GRADLE;
            case JAVA   -> com.eyecode.project.ProjectType.JAVA;
        };
    }
}
