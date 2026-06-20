package com.eyecode.project.template;

import com.eyecode.project.ProjectType;
import com.eyecode.ui.designsystem.IconManager;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class MavenTemplate implements ProjectTemplate {

    @Override
    public ProjectType getType() { return ProjectType.MAVEN; }

    @Override
    public String getDisplayName() { return "Maven"; }

    @Override
    public Icon getIcon() { return IconManager.forProjectType(ProjectType.MAVEN); }

    @Override
    public String getShortDescription() { return "Dependency management and builds"; }

    @Override
    public String getDescription() {
        return "A Maven project using pom.xml for build configuration. "
             + "Maven manages dependencies and provides a standardized project structure.";
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
            "pom.xml"
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

        File pom = new File(root, "pom.xml");
        Files.writeString(pom.toPath(), String.format("""
            <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
                     xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                     xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                <modelVersion>4.0.0</modelVersion>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>1.0-SNAPSHOT</version>
                <properties>
                    <maven.compiler.source>%s</maven.compiler.source>
                    <maven.compiler.target>%s</maven.compiler.target>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                </properties>
            </project>
            """, config.getPackageBase(), config.getName(),
                    config.getJdkVersion(), config.getJdkVersion()));

        File mainFile = new File(mainJava, "Main.java");
        Files.writeString(mainFile.toPath(), String.format("""
            package %s;

            public class Main {
                public static void main(String[] args) {
                    System.out.println("Hello Maven!");
                }
            }
            """, config.getPackageBase()));
    }
}
