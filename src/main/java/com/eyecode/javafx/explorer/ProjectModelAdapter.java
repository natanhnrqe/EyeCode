package com.eyecode.javafx.explorer;

import com.eyecode.project.model.ProjectModel;
import javafx.scene.control.TreeItem;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public final class ProjectModelAdapter {

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git", ".idea", ".gradle", "target", "build", "out"
    );

    public TreeItem<ProjectNode> toTree(ProjectModel model) {
        Path rootPath = model.getRootDir().toAbsolutePath().normalize();
        ProjectNode rootValue = new ProjectNode(model.getName(), rootPath, ProjectNodeType.PROJECT);
        return lazyItem(rootValue);
    }

    private TreeItem<ProjectNode> lazyItem(ProjectNode value) {
        return new LazyTreeItem(value, () -> loadChildren(value), isExpandable(value));
    }

    private boolean isExpandable(ProjectNode value) {
        return value.isDirectory() && hasVisibleChildren(value.path());
    }

    private boolean hasVisibleChildren(Path directory) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (isVisible(entry)) {
                    return true;
                }
            }
            return false;
        } catch (IOException ignored) {
            return false;
        }
    }

    private List<TreeItem<ProjectNode>> loadChildren(ProjectNode parent) {
        List<Path> paths = visibleChildren(parent.path());
        List<TreeItem<ProjectNode>> items = new ArrayList<>(paths.size());
        for (Path child : paths) {
            boolean isDirectory = Files.isDirectory(child, LinkOption.NOFOLLOW_LINKS);
            ProjectNode node = new ProjectNode(
                    child.getFileName().toString(),
                    child.toAbsolutePath().normalize(),
                    isDirectory ? ProjectNodeType.DIRECTORY : ProjectNodeType.FILE);
            items.add(lazyItem(node));
        }
        return items;
    }

    private List<Path> visibleChildren(Path directory) {
        List<Path> children = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory)) {
            for (Path entry : stream) {
                if (isVisible(entry)) {
                    children.add(entry);
                }
            }
        } catch (IOException ignored) {
        }
        children.sort(Comparator
                .comparing((Path path) -> !Files.isDirectory(path))
                .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER));
        return children;
    }

    private boolean isVisible(Path entry) {
        return !Files.isDirectory(entry, LinkOption.NOFOLLOW_LINKS)
                || !IGNORED_DIRECTORIES.contains(entry.getFileName().toString());
    }
}
