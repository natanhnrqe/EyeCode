package com.eyecode.explorer.builder;

import com.eyecode.explorer.model.ExplorerNode;
import com.eyecode.explorer.model.ExplorerNodeType;
import com.eyecode.explorer.model.ExplorerTree;
import com.eyecode.project.model.ProjectModel;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

public class ExplorerTreeBuilder {

    private static final Set<String> IGNORED_DIRECTORIES = Set.of(
            ".git",
            ".idea",
            "target",
            "build",
            "out"
    );

    public ExplorerTree build(ProjectModel model) {
        Path rootPath = model.getRootDir().toAbsolutePath().normalize();
        ExplorerNode root = new ExplorerNode(model.getName(), rootPath, ExplorerNodeType.PROJECT);
        buildChildren(root, rootPath);
        return new ExplorerTree(root);
    }

    private void buildChildren(ExplorerNode parent, Path directory) {
        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> children = stream
                    .filter(this::isVisible)
                    .sorted(directoryFirstThenName())
                    .toList();

            for (Path childPath : children) {
                ExplorerNode child = createNode(childPath);
                parent.addChild(child);
                if (Files.isDirectory(childPath)) {
                    buildChildren(child, childPath);
                }
            }
        } catch (IOException ignored) {
            // Unreadable directories are omitted from the domain tree.
        }
    }

    private ExplorerNode createNode(Path path) {
        ExplorerNodeType type = Files.isDirectory(path)
                ? ExplorerNodeType.DIRECTORY
                : ExplorerNodeType.FILE;
        return new ExplorerNode(path.getFileName().toString(), path.toAbsolutePath().normalize(), type);
    }

    private boolean isVisible(Path path) {
        return !Files.isDirectory(path) || !IGNORED_DIRECTORIES.contains(path.getFileName().toString());
    }

    private Comparator<Path> directoryFirstThenName() {
        return Comparator
                .comparing((Path path) -> !Files.isDirectory(path))
                .thenComparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER);
    }
}
