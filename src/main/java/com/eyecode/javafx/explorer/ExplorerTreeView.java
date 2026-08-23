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
            cell.setContextMenu(ExplorerContextMenu.create());
            cell.setOnMouseClicked(event -> {
                ProjectNode selected = cell.getItem();
                if (event.getClickCount() == 2 && selected != null
                        && selected.type() == ProjectNodeType.FILE
                        && fileOpenHandler != null) {
                    fileOpenHandler.accept(selected.path());
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
