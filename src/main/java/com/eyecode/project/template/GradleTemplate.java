package com.eyecode.project.template;

import com.eyecode.project.ProjectType;
import com.eyecode.ui.designsystem.IconManager;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class GradleTemplate implements ProjectTemplate {

    @Override
    public ProjectType getType() { return ProjectType.GRADLE; }

    @Override
    public String getDisplayName() { return "Gradle"; }

    @Override
    public Icon getIcon() { return IconManager.forProjectType(ProjectType.GRADLE); }

    @Override
    public String getShortDescription() { return "Modern build automation"; }

    @Override
    public String getDescription() {
        return "A Gradle project using build.gradle for build configuration. "
             + "Gradle uses a flexible Groovy DSL for building and testing projects.";
    }

    @Override
    public List<String> getStructurePreview() {
        return List.of(
            "src/",
            "  main/",
            "    java/",
            "    resources/",
            "  test/",
            "    java/",
            "build.gradle"
        );
    }

    @Override
    public void generate(File root, ProjectConfig config) throws IOException {
        File mainJava = new File(root, "src/main/java/" + config.getPackagePath());
        File testJava = new File(root, "src/test/java/" + config.getPackagePath());
        File resources = new File(root, "src/main/resources");
        Files.createDirectories(mainJava.toPath());
        Files.createDirectories(testJava.toPath());
        Files.createDirectories(resources.toPath());

        File build = new File(root, "build.gradle");
        Files.writeString(build.toPath(), String.format("""
            plugins {
                id 'java'
                id 'application'
            }

            group = '%s'
            version = '1.0-SNAPSHOT'

            repositories {
                mavenCentral()
            }

            dependencies {
                testImplementation platform('org.junit:junit-bom:5.10.0')
                testImplementation 'org.junit.jupiter:junit-jupiter'
            }

            application {
                mainClass = '%s.Main'
            }

            tasks.named('test') {
                useJUnitPlatform()
            }
            """, config.getPackageBase(), config.getPackageBase()));

        File mainFile = new File(mainJava, "Main.java");
        Files.writeString(mainFile.toPath(), String.format("""
            package %s;

            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello Gradle!");
                }
            }
            """, config.getPackageBase()));
    }
}
