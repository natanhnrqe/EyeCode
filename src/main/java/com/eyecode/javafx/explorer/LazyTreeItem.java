package com.eyecode.javafx.explorer;

import javafx.scene.control.TreeItem;

import java.util.List;
import java.util.function.Supplier;

final class LazyTreeItem extends TreeItem<ProjectNode> {

    private final Supplier<List<TreeItem<ProjectNode>>> loader;
    private boolean loaded;

    LazyTreeItem(ProjectNode value,
                 Supplier<List<TreeItem<ProjectNode>>> loader,
                 boolean expandable) {
        super(value);
        this.loader = loader;
        if (expandable) {
            getChildren().add(new TreeItem<>(null));
        }
        expandedProperty().addListener((observable, wasExpanded, isExpanded) -> {
            if (isExpanded) {
                load();
            }
        });
    }

    void load() {
        if (loaded) {
            return;
        }
        loaded = true;
        getChildren().setAll(loader.get());
    }

    boolean isLoaded() {
        return loaded;
    }

    boolean hasPlaceholder() {
        return getChildren().size() == 1 && getChildren().get(0).getValue() == null;
    }
}