package com.eyecode.project;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.List;

public class ProjectService {

    private static final int MAX_RECENT = 10;
    private static final String STORAGE_FILE = ".eyecode/recent-projects.dat";
    private static final Set<String> LEGACY_TEST_FIXTURE_PREFIXES = Set.of(
            "eyecode-run-java",
            "eyecode-run-maven",
            "eyecode-run-gradle",
            "eyecode-run-main",
            "eyecode-run-service",
            "eyecode-run-stop");

    private final List<ProjectInfo> recentProjects;
    private final Path storagePath;

    public ProjectService() {
        this(Paths.get(System.getProperty("user.home"), STORAGE_FILE));
    }

    public ProjectService(Path storagePath) {
        if (storagePath == null) {
            throw new IllegalArgumentException("storagePath must not be null");
        }
        this.storagePath = storagePath.toAbsolutePath().normalize();
        this.recentProjects = new ArrayList<>();
        load();
    }

    public List<ProjectInfo> getRecentProjects() {
        return recentProjects.stream()
                .sorted(Comparator.comparingLong(ProjectInfo::getLastOpened).reversed())
                .toList();
    }

    public void addRecent(ProjectInfo project) {
        if (project == null || project.getPath() == null) {
            return;
        }
        String normalizedPath = normalize(project.getPath());
        recentProjects.removeIf(existing -> normalize(existing.getPath()).equals(normalizedPath));
        recentProjects.add(0, new ProjectInfo(
                project.getName(), normalizedPath, project.getType(), System.currentTimeMillis()));
        if (recentProjects.size() > MAX_RECENT) {
            recentProjects.remove(recentProjects.size() - 1);
        }
        save();
    }

    public void removeRecent(String path) {
        if (path == null) {
            return;
        }
        String normalizedPath = normalize(path);
        recentProjects.removeIf(p -> normalize(p.getPath()).equals(normalizedPath));
        save();
    }

    public ProjectInfo findByPath(String path) {
        if (path == null) {
            return null;
        }
        String normalizedPath = normalize(path);
        return recentProjects.stream()
                .filter(p -> normalize(p.getPath()).equals(normalizedPath))
                .findFirst()
                .orElse(null);
    }

    public ProjectInfo findByNameOrPath(String nameOrPath) {
        for (ProjectInfo p : recentProjects) {
            if (p.getName().equals(nameOrPath) || p.getPath().contains(nameOrPath)) {
                return p;
            }
        }
        return null;
    }

    public void save() {
        try {
            Files.createDirectories(storagePath.getParent());
            try (ObjectOutputStream oos = new ObjectOutputStream(
                    new BufferedOutputStream(Files.newOutputStream(storagePath)))) {
                oos.writeObject(new ArrayList<>(recentProjects));
            }
        } catch (IOException e) {
            System.err.println("Failed to save recent projects: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(storagePath)) return;
        boolean changed = false;
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(storagePath)))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                recentProjects.clear();
                for (Object item : (List<?>) obj) {
                    if (item instanceof ProjectInfo info) {
                        String normalizedPath = normalize(info.getPath());
                        if (normalizedPath == null) {
                            changed = true;
                            continue;
                        }
                        Path path = Paths.get(normalizedPath);
                        if (isLegacyEyeCodeTestFixture(path) || !Files.exists(path)) {
                            changed = true;
                            continue;
                        }
                        if (recentProjects.stream()
                                .noneMatch(existing -> normalize(existing.getPath()).equals(normalizedPath))) {
                            recentProjects.add(new ProjectInfo(
                                    info.getName(), normalizedPath, info.getType(), info.getLastOpened()));
                        } else {
                            changed = true;
                        }
                    } else {
                        changed = true;
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load recent projects: " + e.getMessage());
        }
        if (changed) {
            save();
        }
    }

    static boolean isLegacyEyeCodeTestFixture(Path path) {
        if (path == null || path.getFileName() == null) {
            return false;
        }
        Path tempRoot = Paths.get(System.getProperty("java.io.tmpdir"))
                .toAbsolutePath().normalize();
        Path normalized = path.toAbsolutePath().normalize();
        if (!normalized.startsWith(tempRoot)) {
            return false;
        }
        String name = normalized.getFileName().toString();
        return LEGACY_TEST_FIXTURE_PREFIXES.stream().anyMatch(name::startsWith);
    }

    private String normalize(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        return Paths.get(path).toAbsolutePath().normalize().toString();
    }
}
