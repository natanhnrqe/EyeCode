package com.eyecode.challenge;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.regex.Pattern;

public final class ChallengeWorkspaceService {
    public record EnsureResult(Path path, boolean fresh) {
    }

    private static final Pattern ID_PATTERN = Pattern.compile("[a-z0-9-]{1,64}");

    private final Path root;

    public ChallengeWorkspaceService() {
        this(Path.of(System.getProperty("user.home"), ".eyecode", "challenges"));
    }

    public ChallengeWorkspaceService(Path root) {
        this.root = Objects.requireNonNull(root, "root");
    }

    public synchronized EnsureResult ensure(String id) {
        Path directory = challengeDirectory(id).toAbsolutePath().normalize();
        boolean fresh = !Files.isRegularFile(entryFile(directory, classNameOf(id)));
        if (fresh) scaffold(directory, id);
        return new EnsureResult(directory, fresh);
    }

    public synchronized String mainFilePath(String id) {
        Path directory = challengeDirectory(id).toAbsolutePath().normalize();
        return directory.resolve(mainClassPath(classNameOf(id))).toString();
    }

    public synchronized boolean exists(String id) {
        Path directory = challengeDirectory(id);
        return Files.isRegularFile(entryFile(directory, classNameOf(id)));
    }

    public synchronized void reset(String id) {
        Path directory = challengeDirectory(id);
        String className = classNameOf(id);
        write(directory.resolve(mainClassPath(className)), starterSource(className));
        write(directory.resolve(testClassPath(className)), testSource(className));
    }

    private Path challengeDirectory(String id) {
        requireValidId(id);
        return root.resolve(id);
    }

    private static void scaffold(Path directory, String id) {
        String className = classNameOf(id);
        write(directory.resolve("pom.xml"), pomSource(id));
        write(directory.resolve(mainClassPath(className)), starterSource(className));
        write(directory.resolve(testClassPath(className)), testSource(className));
    }

    private static Path entryFile(Path directory, String className) {
        return directory.resolve(mainClassPath(className));
    }

    private static String mainClassPath(String className) {
        return "src/main/java/br/com/eyecode/challenge/" + className + ".java";
    }

    private static String testClassPath(String className) {
        return "src/test/java/br/com/eyecode/challenge/" + className + "Test.java";
    }

    static void requireValidId(String id) {
        if (id == null || !ID_PATTERN.matcher(id).matches()) {
            throw new IllegalArgumentException("Invalid challenge id: " + id);
        }
    }

    static String classNameOf(String id) {
        StringBuilder name = new StringBuilder();
        for (String part : id.split("-")) {
            if (part.isEmpty()) continue;
            name.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) name.append(part.substring(1));
        }
        return name.toString();
    }

    static String pomSource(String id) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                  <modelVersion>4.0.0</modelVersion>
                  <groupId>br.com.eyecode.challenge</groupId>
                  <artifactId>%s</artifactId>
                  <version>1.0.0</version>
                  <packaging>jar</packaging>
                  <properties>
                    <maven.compiler.release>21</maven.compiler.release>
                    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                  </properties>
                  <dependencies>
                    <dependency>
                      <groupId>org.junit.jupiter</groupId>
                      <artifactId>junit-jupiter</artifactId>
                      <version>5.10.2</version>
                      <scope>test</scope>
                    </dependency>
                  </dependencies>
                </project>
                """.formatted(id);
    }

    static String starterSource(String className) {
        return """
                package br.com.eyecode.challenge;

                public class %s {

                    public boolean solve(String input) {
                        return false;
                    }
                }
                """.formatted(className);
    }

    static String testSource(String className) {
        return """
                package br.com.eyecode.challenge;

                import org.junit.jupiter.api.Test;

                class %sTest {

                    @Test
                    void hiddenTestCase() {
                    }
                }
                """.formatted(className);
    }

    private static void write(Path file, String content) {
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write challenge file: " + file, exception);
        }
    }
}
