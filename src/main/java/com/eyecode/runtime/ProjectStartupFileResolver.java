package com.eyecode.runtime;

import com.eyecode.project.model.ProjectModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ProjectStartupFileResolver {
    private final RunConfigurationDiscoveryService discoveryService;
    private final RunConfigurationSelectionStore selectionStore;

    public ProjectStartupFileResolver() {
        this(new RunConfigurationDiscoveryService(), new RunConfigurationSelectionStore());
    }

    public ProjectStartupFileResolver(RunConfigurationDiscoveryService discoveryService,
                                      RunConfigurationSelectionStore selectionStore) {
        this.discoveryService = discoveryService == null ? new RunConfigurationDiscoveryService() : discoveryService;
        this.selectionStore = selectionStore == null ? new RunConfigurationSelectionStore() : selectionStore;
    }

    public Optional<Path> resolve(ProjectModel project) {
        if (project == null) return Optional.empty();
        List<RunConfiguration> configurations = discoveryService.discover(project);
        String selectedId = selectionStore.selectedId(project.getRootDir());
        Optional<Path> selected = configurations.stream()
                .filter(configuration -> configuration.id().equals(selectedId))
                .map(configuration -> sourceFor(project, configuration.mainClass()))
                .flatMap(Optional::stream)
                .findFirst();
        if (selected.isPresent()) return selected;

        Optional<Path> spring = configurations.stream()
                .filter(configuration -> configuration.kind() == RunConfigurationKind.SPRING_BOOT)
                .map(configuration -> sourceFor(project, configuration.mainClass()))
                .flatMap(Optional::stream)
                .findFirst();
        if (spring.isPresent()) return spring;

        Optional<Path> runnable = discoveryService.defaultConfiguration(configurations)
                .flatMap(configuration -> sourceFor(project, configuration.mainClass()));
        return runnable.isPresent() ? runnable : firstSource(project);
    }

    private Optional<Path> sourceFor(ProjectModel project, String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) return Optional.empty();
        String relative = qualifiedName.replace('.', '/') + ".java";
        return sourceRoots(project).stream()
                .map(root -> root.resolve(relative).normalize())
                .filter(Files::isRegularFile)
                .findFirst();
    }

    private Optional<Path> firstSource(ProjectModel project) {
        for (Path root : sourceRoots(project)) {
            if (!Files.isDirectory(root)) continue;
            try (var paths = Files.walk(root)) {
                Optional<Path> candidate = paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().endsWith(".java"))
                        .filter(this::isSensibleSource)
                        .sorted(Comparator.comparing(path -> path.toAbsolutePath().normalize().toString()))
                        .findFirst();
                if (candidate.isPresent()) return candidate.map(path -> path.toAbsolutePath().normalize());
            } catch (IOException ignored) {
            }
        }
        return Optional.empty();
    }

    private List<Path> sourceRoots(ProjectModel project) {
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Set<Path> roots = new LinkedHashSet<>();
        roots.add(root.resolve("src/main/java"));
        roots.add(root.resolve("src"));
        return new ArrayList<>(roots);
    }

    private boolean isSensibleSource(Path path) {
        for (Path part : path) {
            String name = part.toString();
            if (name.equals("test") || name.equals("target") || name.equals("build") || name.equals("generated")
                    || name.equals(".eyecode")) {
                return false;
            }
        }
        return true;
    }
}
