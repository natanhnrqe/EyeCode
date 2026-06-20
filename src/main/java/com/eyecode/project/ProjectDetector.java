package com.eyecode.project;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public final class ProjectDetector {

    private ProjectDetector() {}

    public static ProjectType detect(File folder) {
        if (folder == null || !folder.isDirectory()) {
            return ProjectType.UNKNOWN;
        }
        if (isSpringBoot(folder)) return ProjectType.SPRING_BOOT;
        if (hasPomXml(folder))    return ProjectType.MAVEN;
        if (hasGradle(folder))    return ProjectType.GRADLE;
        if (hasGit(folder))       return ProjectType.GIT;
        if (hasSrc(folder))       return ProjectType.JAVA;
        return ProjectType.UNKNOWN;
    }

    private static boolean isSpringBoot(File folder) {
        File pom = new File(folder, "pom.xml");
        if (!pom.exists()) return false;
        try {
            String content = Files.readString(pom.toPath(), StandardCharsets.UTF_8);
            return content.contains("<groupId>org.springframework.boot")
                || content.contains("<artifactId>spring-boot-starter")
                || content.contains("spring-boot-maven-plugin");
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean hasPomXml(File folder) {
        return new File(folder, "pom.xml").exists();
    }

    private static boolean hasGradle(File folder) {
        return new File(folder, "build.gradle").exists()
            || new File(folder, "build.gradle.kts").exists();
    }

    private static boolean hasGit(File folder) {
        return new File(folder, ".git").exists();
    }

    private static boolean hasSrc(File folder) {
        File src = new File(folder, "src");
        return src.exists() && src.isDirectory();
    }
}
