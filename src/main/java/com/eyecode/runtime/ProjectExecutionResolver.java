package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ProjectExecutionResolver {

    private final BuildToolExecutableResolver buildToolResolver;
    private final MavenClasspathResolver mavenClasspathResolver;

    public ProjectExecutionResolver() {
        this(new BuildToolExecutableResolver());
    }

    ProjectExecutionResolver(BuildToolExecutableResolver buildToolResolver) {
        this.buildToolResolver = buildToolResolver == null ? new BuildToolExecutableResolver() : buildToolResolver;
        this.mavenClasspathResolver = new MavenClasspathResolver(this.buildToolResolver);
    }

    public ResolvedExecution resolve(ProjectModel project) {
        if (project == null) {
            throw new IllegalArgumentException("No project is open");
        }
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Path pom = root.resolve("pom.xml");
        Path gradle = existing(root, "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts");
        if (pom.toFile().isFile() && isSpringMaven(pom)) {
            return new ResolvedExecution(ResolvedExecution.Kind.SPRING_MAVEN,
                    List.of(buildToolResolver.mavenCommand(root, "spring-boot:run")), null);
        }
        if (gradle != null && isSpringGradle(root)) {
            return new ResolvedExecution(ResolvedExecution.Kind.SPRING_GRADLE,
                    List.of(buildToolResolver.gradleCommand(root, "bootRun")), null);
        }
        RunConfigurationDiscoveryService discovery = new RunConfigurationDiscoveryService();
        List<RunConfiguration> configurations = discovery.discover(project);
        if (configurations.isEmpty()) {
            throw new IllegalArgumentException("No main class found");
        }
        if (configurations.size() > 1) {
            throw new IllegalArgumentException("Multiple main classes found: "
                    + configurations.stream().map(RunConfiguration::mainClass).sorted().toList());
        }
        return resolve(project, configurations.getFirst());
    }

    public ResolvedExecution resolve(ProjectModel project, RunConfiguration configuration) {
        if (project == null || configuration == null) {
            throw new IllegalArgumentException("Project and run configuration are required");
        }
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Path pom = root.resolve("pom.xml");
        Path gradle = existing(root, "build.gradle", "build.gradle.kts", "settings.gradle", "settings.gradle.kts");
        if (pom.toFile().isFile() && isSpringMaven(pom)) {
            return new ResolvedExecution(ResolvedExecution.Kind.SPRING_MAVEN,
                    List.of(buildToolResolver.mavenCommand(root, "spring-boot:run",
                            "-Dspring-boot.run.main-class=" + configuration.mainClass())), configuration.mainClass());
        }
        if (gradle != null && isSpringGradle(root)) {
            return new ResolvedExecution(ResolvedExecution.Kind.SPRING_GRADLE,
                    List.of(buildToolResolver.gradleCommand(root, "bootRun",
                            "-Dspring-boot.run.main-class=" + configuration.mainClass())), configuration.mainClass());
        }
        if (pom.toFile().isFile()) {
            return mavenJavaApplication(root, configuration.mainClass());
        }
        if (gradle != null) {
            return new ResolvedExecution(ResolvedExecution.Kind.GRADLE,
                    List.of(buildToolResolver.gradleCommand(root, "run",
                            "-PmainClass=" + configuration.mainClass())), configuration.mainClass());
        }
        return standardJava(root, configuration.mainClass());
    }

    private ResolvedExecution standardJava(Path root, String mainClass) {
        Path sourceRoot = Files.isDirectory(root.resolve("src/main/java"))
                ? root.resolve("src/main/java") : root.resolve("src");
        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("No Java source root found");
        }
        Path output = root.resolve(".eyecode/out");
        Path entrySource = entrySource(sourceRoot, mainClass);
        String classpath = classpath(output, root);
        List<String> compile = new ArrayList<>(List.of(
                "javac",
                "-cp", classpath,
                "-sourcepath", sourceRoot.toString(),
                "-Xprefer:source",
                "-d", output.toString(),
                entrySource.toString()));
        List<String> launch = List.of("java", "-cp", classpath, mainClass);
        return new ResolvedExecution(ResolvedExecution.Kind.STANDARD_JAVA,
                List.of(compile, launch), mainClass);
    }

    private ResolvedExecution mavenJavaApplication(Path root, String mainClass) {
        Path sourceRoot = root.resolve("src/main/java");
        if (!Files.isDirectory(sourceRoot)) {
            throw new IllegalArgumentException("No Maven Java source root found");
        }
        Path output = root.resolve(".eyecode/out");
        Path resources = root.resolve("src/main/resources");
        String dependencies = mavenClasspathResolver.resolve(root);
        String classpath = classpath(output, resources, dependencies, root);
        Path entrySource = entrySource(sourceRoot, mainClass);
        List<String> compile = new ArrayList<>(List.of(
                "javac",
                "-cp", classpath,
                "-sourcepath", sourceRoot.toString(),
                "-Xprefer:source",
                "-d", output.toString(),
                entrySource.toString()));
        List<String> launch = List.of("java", "-cp", classpath, mainClass);
        return new ResolvedExecution(ResolvedExecution.Kind.MAVEN_JAVA_APPLICATION,
                List.of(compile, launch), mainClass);
    }

    private Path entrySource(Path sourceRoot, String mainClass) {
        String relative = mainClass.replace('.', sourceRoot.getFileSystem().getSeparator().charAt(0)) + ".java";
        Path entry = sourceRoot.resolve(relative).normalize();
        if (!entry.startsWith(sourceRoot) || !Files.isRegularFile(entry)) {
            throw new IllegalArgumentException("Main source not found for " + mainClass);
        }
        return entry;
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

    private String classpath(Path output, Path resources, String dependencies, Path root) {
        StringBuilder classpath = new StringBuilder(output.toString());
        if (Files.isDirectory(resources)) {
            classpath.append(System.getProperty("path.separator")).append(resources);
        }
        if (dependencies != null && !dependencies.isBlank()) {
            classpath.append(System.getProperty("path.separator")).append(dependencies);
        }
        appendLibraries(classpath, root);
        return classpath.toString();
    }

    private void appendLibraries(StringBuilder classpath, Path root) {
        Path libs = root.resolve("libs");
        if (!Files.isDirectory(libs)) return;
        try (var stream = Files.list(libs)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .sorted()
                    .forEach(path -> classpath.append(System.getProperty("path.separator")).append(path));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Unable to scan project libraries", exception);
        }
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
}
