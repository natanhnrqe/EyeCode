package com.eyecode.javafx.explorer;

import com.eyecode.project.model.ProjectModel;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyCode;
import javafx.application.Platform;

import java.nio.file.Path;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public final class ExplorerTreeView extends TreeView<ProjectNode> {

    private final Map<Path, TreeItem<ProjectNode>> pathIndex = new HashMap<>();
    private ProjectModel model;
    private ProjectModelAdapter adapter;

    public ExplorerTreeView(ProjectModel model) {
        this(model, path -> { });
    }

    public ExplorerTreeView(ProjectModel model, Consumer<Path> fileOpenHandler) {
        this(model, fileOpenHandler, request -> { });
    }

    public ExplorerTreeView(ProjectModel model, Consumer<Path> fileOpenHandler,
                            Consumer<ExplorerNewRequest> newActionHandler) {
        this(model, fileOpenHandler, newActionHandler, item -> { }, item -> { });
    }

    public ExplorerTreeView(ProjectModel model, Consumer<Path> fileOpenHandler,
                            Consumer<ExplorerNewRequest> newActionHandler,
                            Consumer<ProjectNode> renameAction,
                            Consumer<ProjectNode> deleteAction) {
        getStyleClass().add("explorer-tree-view");
        setShowRoot(true);

        installModel(model, Set.of(), null);

        setCellFactory(view -> {
            ExplorerRow row = new ExplorerRow();
            TreeCell<ProjectNode> cell = new TreeCell<>() {
                @Override
                protected void updateItem(ProjectNode item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        row.bindExpanded(null);
                        setText(null);
                        setGraphic(null);
                        return;
                    }
                    row.bindExpanded(getTreeItem());
                    row.update(ExplorerRowViewModel.from(getTreeItem(), isSelected()));
                    setGraphic(row);
                }
            };
            cell.getStyleClass().add("explorer-row-cell");
            cell.setDisclosureNode(row.getArrowRegion());
            cell.setOnMouseClicked(event -> {
                ProjectNode selected = cell.getItem();
                if (event.getClickCount() == 2 && selected != null
                        && selected.type() == ProjectNodeType.FILE
                        && fileOpenHandler != null) {
                    fileOpenHandler.accept(selected.path());
                }
            });
            cell.setOnContextMenuRequested(event -> {
                if (cell.getItem() != null) {
                    ExplorerContextMenu.create(cell.getItem(), newActionHandler, renameAction, deleteAction)
                            .show(cell, event.getScreenX(), event.getScreenY());
                    event.consume();
                }
            });
            return cell;
        });
        addEventHandler(javafx.scene.input.KeyEvent.KEY_PRESSED, event -> {
            TreeItem<ProjectNode> selectedItem = getSelectionModel().getSelectedItem();
            ProjectNode selected = selectedItem == null ? null : selectedItem.getValue();
            if (selected == null) return;
            if (event.getCode() == KeyCode.F2) {
                renameAction.accept(selected);
                event.consume();
            } else if (event.getCode() == KeyCode.DELETE) {
                deleteAction.accept(selected);
                event.consume();
            }
        });
    }

    public void reloadProject(ProjectModel model) {
        Set<Path> expanded = expandedPaths();
        Path selected = selectedPath();
        installModel(model, expanded, selected);
    }

    public void applyPathChange(Path path) {
        if (path == null) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> applyPathChange(path));
            return;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (model == null || !normalized.startsWith(model.getRootDir().toAbsolutePath().normalize())) return;
        reindex();
        if (!Files.exists(normalized) || !adapter.isVisible(normalized)) {
            removePath(normalized);
            return;
        }
        addPath(normalized);
    }

    public void applyRename(Path oldPath, Path newPath) {
        if (oldPath == null || newPath == null) return;
        if (!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> applyRename(oldPath, newPath));
            return;
        }
        Path oldNormalized = oldPath.toAbsolutePath().normalize();
        Path newNormalized = newPath.toAbsolutePath().normalize();
        Path root = model == null ? null : model.getRootDir().toAbsolutePath().normalize();
        if (root == null || !oldNormalized.startsWith(root) || !newNormalized.startsWith(root)) return;
        Set<Path> expanded = renamedPaths(expandedPaths(), oldNormalized, newNormalized);
        Path selected = renamedPath(selectedPath(), oldNormalized, newNormalized);
        removePath(oldNormalized);
        addPath(newNormalized);
        for (Path path : expanded) expandPath(path);
        if (selected != null) {
            TreeItem<ProjectNode> replacement = findOrLoad(selected);
            if (replacement != null) getSelectionModel().select(replacement);
        }
    }

    private void installModel(ProjectModel model, Set<Path> expanded, Path selected) {
        this.model = model;
        this.adapter = new ProjectModelAdapter();
        TreeItem<ProjectNode> root = adapter.toTree(model);
        setRoot(root);
        root.setExpanded(true);
        reindex();
        for (Path path : expanded) expandPath(path);
        if (selected != null) {
            TreeItem<ProjectNode> item = findOrLoad(selected);
            if (item != null) getSelectionModel().select(item);
        }
    }

    private void addPath(Path path) {
        if (!adapter.isVisible(path)) return;
        TreeItem<ProjectNode> parent = ensureParent(path.getParent());
        if (parent == null) return;
        if (parent instanceof LazyTreeItem lazy && !lazy.isLoaded()) return;
        TreeItem<ProjectNode> existing = pathIndex.get(path);
        if (existing != null) return;
        TreeItem<ProjectNode> item = adapter.itemFor(path);
        parent.getChildren().add(item);
        parent.getChildren().sort(adapter.ordering());
        index(item);
    }

    private TreeItem<ProjectNode> ensureParent(Path path) {
        if (path == null) return null;
        TreeItem<ProjectNode> existing = pathIndex.get(path);
        if (existing != null) return existing;
        TreeItem<ProjectNode> parent = ensureParent(path.getParent());
        if (parent == null || !Files.isDirectory(path) || !adapter.isVisible(path)) return null;
        if (parent instanceof LazyTreeItem lazy && !lazy.isLoaded()) lazy.load();
        TreeItem<ProjectNode> created = adapter.itemFor(path);
        parent.getChildren().add(created);
        parent.getChildren().sort(adapter.ordering());
        index(created);
        return created;
    }

    private void removePath(Path path) {
        reindex();
        TreeItem<ProjectNode> item = pathIndex.get(path);
        if (item == null || item == getRoot()) return;
        Path selected = selectedPath();
        TreeItem<ProjectNode> parent = item.getParent();
        parent.getChildren().remove(item);
        removeIndex(item);
        if (selected != null && selected.startsWith(path)) getSelectionModel().select(parent);
    }

    private Set<Path> expandedPaths() {
        Set<Path> result = new HashSet<>();
        collectState(getRoot(), result);
        return result;
    }

    private void collectState(TreeItem<ProjectNode> item, Set<Path> expanded) {
        if (item == null || item.getValue() == null) return;
        if (item.isExpanded()) expanded.add(item.getValue().path());
        for (TreeItem<ProjectNode> child : item.getChildren()) collectState(child, expanded);
    }

    private Set<Path> renamedPaths(Set<Path> paths, Path oldPath, Path newPath) {
        Set<Path> renamed = new HashSet<>();
        for (Path path : paths) {
            renamed.add(renamedPath(path, oldPath, newPath));
        }
        return renamed;
    }

    private Path renamedPath(Path path, Path oldPath, Path newPath) {
        if (path == null || !path.startsWith(oldPath)) return path;
        return newPath.resolve(oldPath.relativize(path));
    }

    private Path selectedPath() {
        TreeItem<ProjectNode> selected = getSelectionModel().getSelectedItem();
        return selected == null || selected.getValue() == null ? null : selected.getValue().path();
    }

    private void expandPath(Path path) {
        TreeItem<ProjectNode> item = findOrLoad(path);
        if (item != null) item.setExpanded(true);
    }

    private TreeItem<ProjectNode> findOrLoad(Path path) {
        TreeItem<ProjectNode> item = pathIndex.get(path);
        if (item != null) return item;
        Path rootPath = getRoot().getValue().path();
        if (!path.startsWith(rootPath)) return null;
        TreeItem<ProjectNode> current = getRoot();
        Path relative = rootPath.relativize(path);
        for (Path part : relative) {
            if (current instanceof LazyTreeItem lazy && !lazy.isLoaded()) lazy.load();
            reindex();
            current = current.getChildren().stream().filter(child -> child.getValue() != null
                    && child.getValue().name().equals(part.toString())).findFirst().orElse(null);
            if (current == null) return null;
        }
        return current;
    }

    private void reindex() {
        pathIndex.clear();
        index(getRoot());
    }

    private void index(TreeItem<ProjectNode> item) {
        if (item == null || item.getValue() == null) return;
        pathIndex.put(item.getValue().path(), item);
        for (TreeItem<ProjectNode> child : item.getChildren()) index(child);
    }

    private void removeIndex(TreeItem<ProjectNode> item) {
        if (item == null || item.getValue() == null) return;
        pathIndex.remove(item.getValue().path());
        for (TreeItem<ProjectNode> child : item.getChildren()) removeIndex(child);
    }
}
