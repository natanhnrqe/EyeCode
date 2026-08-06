package com.eyecode.javafx.explorer;

import javafx.geometry.Pos;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;

public final class ExplorerRow extends HBox {

    private final ArrowRegion arrowRegion = new ArrowRegion();
    private final IconRegion iconRegion = new IconRegion();
    private final TitleRegion titleRegion = new TitleRegion();
    private final BadgeRegion badgeRegion = new BadgeRegion();
    private final StatusRegion statusRegion = new StatusRegion();

    public ExplorerRow() {
        getStyleClass().add("explorer-row");
        setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(titleRegion, Priority.ALWAYS);
        getChildren().addAll(iconRegion, titleRegion, badgeRegion, statusRegion);
    }

    public ArrowRegion getArrowRegion() {
        return arrowRegion;
    }

    public IconRegion getIconRegion() {
        return iconRegion;
    }

    public TitleRegion getTitleRegion() {
        return titleRegion;
    }

    public BadgeRegion getBadgeRegion() {
        return badgeRegion;
    }

    public StatusRegion getStatusRegion() {
        return statusRegion;
    }

    public void update(ExplorerRowViewModel vm) {
        iconRegion.update(vm.icon());
        titleRegion.setText(vm.title());
        badgeRegion.update(vm.badge());
        statusRegion.update(vm.status());
    }

    public void bindExpanded(TreeItem<ProjectNode> item) {
        arrowRegion.expandedProperty().unbind();
        arrowRegion.expandedProperty().set(false);
        if (item != null) {
            arrowRegion.expandedProperty().bind(item.expandedProperty());
        }
    }
}
