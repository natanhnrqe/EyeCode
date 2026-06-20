package com.eyecode.project;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ProjectService {

    private static final int MAX_RECENT = 10;
    private static final String STORAGE_FILE = ".eyecode/recent-projects.dat";

    private final List<ProjectInfo> recentProjects;
    private final Path storagePath;

    public ProjectService() {
        this.storagePath = Paths.get(System.getProperty("user.home"), STORAGE_FILE);
        this.recentProjects = new ArrayList<>();
        load();
    }

    public List<ProjectInfo> getRecentProjects() {
        return new ArrayList<>(recentProjects);
    }

    public void addRecent(ProjectInfo project) {
        recentProjects.remove(project);
        recentProjects.add(0, project.withLastOpened(System.currentTimeMillis()));
        if (recentProjects.size() > MAX_RECENT) {
            recentProjects.remove(recentProjects.size() - 1);
        }
        save();
    }

    public void removeRecent(String path) {
        recentProjects.removeIf(p -> p.getPath().equals(path));
        save();
    }

    public ProjectInfo findByPath(String path) {
        return recentProjects.stream()
                .filter(p -> p.getPath().equals(path))
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
        try (ObjectInputStream ois = new ObjectInputStream(
                new BufferedInputStream(Files.newInputStream(storagePath)))) {
            Object obj = ois.readObject();
            if (obj instanceof List) {
                recentProjects.clear();
                for (Object item : (List<?>) obj) {
                    if (item instanceof ProjectInfo info) {
                        recentProjects.add(info);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to load recent projects: " + e.getMessage());
        }
    }
}
