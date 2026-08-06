package com.eyecode.javafx.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;

public final class ExplorerContextMenu {

    private ExplorerContextMenu() {}

    public static ContextMenu create() {
        ContextMenu menu = new ContextMenu();
        menu.getItems().addAll(
                item("Open"),
                item("Rename"),
                item("Delete"),
                new javafx.scene.control.SeparatorMenuItem(),
                item("Copy Path"),
                item("Reveal"),
                new javafx.scene.control.SeparatorMenuItem(),
                item("New")
        );
        return menu;
    }

    private static MenuItem item(String text) {
        MenuItem menuItem = new MenuItem(text);
        menuItem.setId("explorer-action-" + text.toLowerCase().replace(' ', '-'));
        return menuItem;
    }
}