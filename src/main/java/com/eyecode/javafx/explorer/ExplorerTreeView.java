package com.eyecode.javafx.explorer;

import com.eyecode.project.model.ProjectModel;
import javafx.scene.control.TreeCell;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;

import java.nio.file.Path;
import java.util.function.Consumer;

public final class ExplorerTreeView extends TreeView<ProjectNode> {

    public ExplorerTreeView(ProjectModel model) {
        this(model, path -> { });
    }

    public ExplorerTreeView(ProjectModel model, Consumer<Path> fileOpenHandler) {
        this(model, fileOpenHandler, request -> { });
    }

    public ExplorerTreeView(ProjectModel model, Consumer<Path> fileOpenHandler,
                            Consumer<ExplorerNewRequest> newActionHandler) {
        getStyleClass().add("explorer-tree-view");
        setShowRoot(true);

        ProjectModelAdapter adapter = new ProjectModelAdapter();
        TreeItem<ProjectNode> root = adapter.toTree(model);
        setRoot(root);
        root.setExpanded(true);

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
                    ExplorerContextMenu.create(cell.getItem(), newActionHandler)
                            .show(cell, event.getScreenX(), event.getScreenY());
                    event.consume();
                }
            });
            return cell;
        });
    }

    public void reloadProject(ProjectModel model) {
        ProjectModelAdapter adapter = new ProjectModelAdapter();
        TreeItem<ProjectNode> root = adapter.toTree(model);
        setRoot(root);
        root.setExpanded(true);
    }
}
