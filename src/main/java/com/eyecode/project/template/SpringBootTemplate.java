package com.eyecode.project.template;

import com.eyecode.project.ProjectType;
import com.eyecode.ui.designsystem.IconManager;
import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

public class SpringBootTemplate implements ProjectTemplate {

    @Override
    public ProjectType getType() { return ProjectType.SPRING_BOOT; }

    @Override
    public String getDisplayName() { return "Spring Boot"; }

    @Override
    public Icon getIcon() { return IconManager.forProjectType(ProjectType.SPRING_BOOT); }

    @Override
    public String getShortDescription() { return "Web APIs and enterprise applications"; }

    @Override
    public String getDescription() {
        return "A Spring Boot project using Maven. "
             + "Spring Boot provides auto-configuration and embedded servers for rapid development.";
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
            "pom.xml",
            "application.properties"
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
                <parent>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-parent</artifactId>
                    <version>3.2.0</version>
                </parent>
                <groupId>%s</groupId>
                <artifactId>%s</artifactId>
                <version>1.0-SNAPSHOT</version>
                <properties>
                    <java.version>%s</java.version>
                </properties>
                <dependencies>
                    <dependency>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-web</artifactId>
                    </dependency>
                </dependencies>
            </project>
            """, config.getPackageBase(), config.getName(),
                    config.getJdkVersion()));

        File app = new File(mainJava, "Application.java");
        Files.writeString(app.toPath(), String.format("""
            package %s;

            import org.springframework.boot.SpringApplication;
            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class Application {
                public static void main(String[] args) {
                    SpringApplication.run(Application.class, args);
                }
            }
            """, config.getPackageBase()));

        File props = new File(resources, "application.properties");
        Files.writeString(props.toPath(), String.format("""
            spring.application.name=%s
            server.port=8080
            """, config.getName()));
    }
}
