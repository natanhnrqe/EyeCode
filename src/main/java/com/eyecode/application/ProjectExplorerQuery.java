package com.eyecode.application;

import com.eyecode.project.model.ProjectModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public final class ProjectExplorerQuery {
    public record Entry(Path path, String name, boolean directory, boolean hasChildren) {}

    public Path requireDirectory(ProjectModel project, Path directory) {
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Path normalized = directory.toAbsolutePath().normalize();
        if (!normalized.startsWith(root) || !Files.isDirectory(normalized)) {
            throw new IllegalArgumentException("The requested folder is not in the project");
        }
        return normalized;
    }

    public boolean isProjectFile(ProjectModel project, Path path) {
        return project != null && path.toAbsolutePath().normalize().startsWith(
                project.getRootDir().toAbsolutePath().normalize()) && Files.isRegularFile(path);
    }

    public List<Path> validExpandedDirectories(ProjectModel project, List<Path> paths) {
        Path root = project.getRootDir().toAbsolutePath().normalize();
        List<Path> valid = paths.stream().map(path -> path.toAbsolutePath().normalize())
                .filter(path -> path.startsWith(root) && Files.isDirectory(path)).toList();
        return valid.isEmpty() ? List.of(root) : valid;
    }

    public List<Path> ancestors(ProjectModel project, Path target) {
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Path normalized = target.toAbsolutePath().normalize();
        if (!normalized.startsWith(root)) throw new IllegalArgumentException("Path is not in the project");
        List<Path> result = new ArrayList<>();
        for (Path current = normalized.getParent(); current != null && current.startsWith(root)
                && !current.equals(root); current = current.getParent()) result.add(current);
        java.util.Collections.reverse(result);
        return List.copyOf(result);
    }

    public Optional<Path> changedParent(ProjectModel project, Path changed) {
        if (project == null || changed == null) return Optional.empty();
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Path normalized = changed.toAbsolutePath().normalize();
        Path parent = normalized.getParent();
        return normalized.startsWith(root) && parent != null && parent.startsWith(root)
                ? Optional.of(parent) : Optional.empty();
    }

    public Optional<Path> preferredEntryPoint(ProjectModel project, Optional<String> mainClass) {
        Optional<Path> configured = mainClass
                .flatMap(name -> sourceFor(project, name));
        if (configured.isPresent()) return configured;
        for (Path sourceRoot : sourceRoots(project)) {
            if (!Files.isDirectory(sourceRoot)) continue;
            try (var paths = Files.walk(sourceRoot, 12)) {
                Optional<Path> main = paths.filter(Files::isRegularFile)
                        .filter(path -> path.getFileName().toString().equals("Main.java"))
                        .filter(this::isSensibleSource)
                        .sorted(Comparator.comparing(path -> path.toAbsolutePath().normalize().toString()))
                        .findFirst();
                if (main.isPresent()) return main.map(path -> path.toAbsolutePath().normalize());
            } catch (IOException ignored) {
            }
        }
        return Optional.empty();
    }

    private Optional<Path> sourceFor(ProjectModel project, String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) return Optional.empty();
        String relative = qualifiedName.replace('.', File.separatorChar) + ".java";
        return sourceRoots(project).stream().map(root -> root.resolve(relative).normalize())
                .filter(Files::isRegularFile).findFirst();
    }

    private List<Path> sourceRoots(ProjectModel project) {
        Path root = project.getRootDir().toAbsolutePath().normalize();
        Path standard = root.resolve("src/main/java");
        return Files.isDirectory(standard) ? List.of(standard) : List.of(root.resolve("src"));
    }

    private boolean isSensibleSource(Path path) {
        for (Path part : path) {
            if (Set.of("target", "build", "out", ".gradle", ".idea", "node_modules", ".git").contains(part.toString())) {
                return false;
            }
        }
        return true;
    }

    public Entry node(Path path) {
        Path normalized = path.toAbsolutePath().normalize();
        boolean directory = Files.isDirectory(normalized);
        return new Entry(normalized, normalized.getFileName().toString(), directory,
                directory && hasVisibleChildren(normalized));
    }

    public List<Entry> children(ProjectModel project, Path directory) {
        directory = requireDirectory(project, directory);
        try (var stream = Files.list(directory)) {
            return stream.filter(this::isVisibleProjectPath)
                    .sorted(Comparator
                            .comparing((Path path) -> !Files.isDirectory(path))
                            .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                    .map(this::node)
                    .toList();
        } catch (IOException ignored) {
            return List.of();
        }
    }

    private boolean hasVisibleChildren(Path directory) {
        try (var stream = Files.list(directory)) {
            return stream.anyMatch(this::isVisibleProjectPath);
        } catch (IOException ignored) {
            return false;
        }
    }

    private boolean isVisibleProjectPath(Path path) {
        if (!Files.isDirectory(path)) return true;
        String name = path.getFileName().toString();
        return !Set.of(".git", ".idea", ".gradle", ".eyecode", "target", "build", "out").contains(name);
    }
}
