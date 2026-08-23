package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;
import com.eyecode.run.MainClassFinder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class ProjectExecutionResolver {

    public ResolvedExecution resolve(ProjectModel project) {
        if (project == null) {
            throw new IllegalArgumentException("No project is open");
        }
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Path pom = root.resolve("pom.xml");
        Path gradle = existing(root, "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts");
        if (pom.toFile().isFile() && isSpringMaven(pom)) {
            return new ResolvedExecution(ResolvedExecution.Kind.SPRING_MAVEN,
                    List.of(toolCommand(root, mavenWrapper(root), "mvn", "spring-boot:run")), null);
        }
        if (gradle != null && isSpringGradle(root)) {
            return new ResolvedExecution(ResolvedExecution.Kind.SPRING_GRADLE,
                    List.of(toolCommand(root, wrapper(root, true), "gradle", "bootRun")), null);
        }
        if (pom.toFile().isFile()) {
            String mainClass = singleMainClass(root);
            return new ResolvedExecution(ResolvedExecution.Kind.MAVEN,
                    List.of(toolCommand(root, mavenWrapper(root), "mvn", "compile", "exec:java",
                            "-Dexec.mainClass=" + mainClass)), mainClass);
        }
        if (gradle != null) {
            return new ResolvedExecution(ResolvedExecution.Kind.GRADLE,
                    List.of(toolCommand(root, wrapper(root, true), "gradle", "run")), null);
        }
        return standardJava(root);
    }

    private ResolvedExecution standardJava(Path root) {
        Path sourceRoot = Files.isDirectory(root.resolve("src/main/java"))
                ? root.resolve("src/main/java") : root.resolve("src");
        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("No Java source root found");
        }
        String mainClass = singleMainClass(sourceRoot);
        Path output = root.resolve(".eyecode/out");
        List<String> files = javaFiles(sourceRoot);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No Java source files found");
        }
        String classpath = classpath(output, root);
        List<String> compile = new ArrayList<>(List.of("javac", "-cp", classpath, "-d", output.toString()));
        compile.addAll(files);
        List<String> launch = List.of("java", "-cp", classpath, mainClass);
        return new ResolvedExecution(ResolvedExecution.Kind.STANDARD_JAVA,
                List.of(compile, launch), mainClass);
    }

    private String singleMainClass(Path root) {
        List<String> mainClasses = new MainClassFinder().findMainClasses(root.toFile());
        if (mainClasses.isEmpty()) {
            throw new IllegalArgumentException("No main class found");
        }
        if (mainClasses.size() > 1) {
            throw new IllegalArgumentException("Multiple main classes found: " + String.join(", ", mainClasses));
        }
        return mainClasses.getFirst();
    }

    private List<String> javaFiles(Path sourceRoot) {
        try (var stream = Files.walk(sourceRoot)) {
            return stream.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".java"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(Path::toString)
                    .toList();
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to scan Java source root", exception);
        }
    }

    private String classpath(Path output, Path root) {
        StringBuilder classpath = new StringBuilder(output.toString());
        Path libs = root.resolve("libs");
        if (Files.isDirectory(libs)) {
            try (var stream = Files.list(libs)) {
                stream.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".jar"))
                        .sorted()
                        .forEach(path -> classpath.append(System.getProperty("path.separator")).append(path));
            } catch (IOException exception) {
                throw new IllegalArgumentException("Unable to scan project libraries", exception);
            }
        }
        return classpath.toString();
    }

    private boolean isSpringMaven(Path pom) {
        return read(pom).contains("spring-boot");
    }

    private boolean isSpringGradle(Path root) {
        for (String name : List.of("build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts")) {
            Path script = root.resolve(name);
            if (Files.isRegularFile(script)) {
                String content = read(script);
                if (content.contains("org.springframework.boot") || content.contains("spring-boot")) {
                    return true;
                }
            }
        }
        return false;
    }

    private String read(Path path) {
        try {
            return Files.readString(path, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private Path existing(Path root, String... names) {
        for (String name : names) {
            Path candidate = root.resolve(name);
            if (Files.isRegularFile(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private String wrapper(Path root, boolean gradle) {
        if (gradle) {
            if (Files.isRegularFile(root.resolve("gradlew.bat"))) return "gradlew.bat";
            if (Files.isRegularFile(root.resolve("gradlew"))) return "./gradlew";
        }
        return "gradle";
    }

    private String mavenWrapper(Path root) {
        if (Files.isRegularFile(root.resolve("mvnw.cmd"))) return "mvnw.cmd";
        if (Files.isRegularFile(root.resolve("mvnw"))) return "mvnw";
        return "mvn";
    }

    private List<String> toolCommand(Path root, String wrapper, String global, String... args) {
        List<String> command = new ArrayList<>();
        if (wrapper.endsWith(".cmd") || wrapper.endsWith(".bat")) {
            command.add("cmd");
            command.add("/c");
            command.add(root.resolve(wrapper).toString());
        } else {
            command.add(Files.exists(root.resolve(wrapper)) ? root.resolve(wrapper).toString() : global);
        }
        command.addAll(List.of(args));
        return command;
    }
}
