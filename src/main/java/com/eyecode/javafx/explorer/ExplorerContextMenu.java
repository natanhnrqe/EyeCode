package com.eyecode.javafx.explorer;

import javafx.scene.control.ContextMenu;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

public final class ExplorerContextMenu {

    private ExplorerContextMenu() {}

    public static ContextMenu create() {
        return create(null, null);
    }

    public static ContextMenu create(ProjectNode node, Consumer<ExplorerNewRequest> newAction) {
        return create(node, newAction, item -> { }, item -> { });
    }

    public static ContextMenu create(ProjectNode node, Consumer<ExplorerNewRequest> newAction,
                                     Consumer<ProjectNode> renameAction,
                                     Consumer<ProjectNode> deleteAction) {
        ContextMenu menu = new ContextMenu();
        Menu newMenu = new Menu("New");
        for (ExplorerNewKind kind : ExplorerNewKind.values()) {
            MenuItem item = new MenuItem(label(kind));
            item.setId("explorer-new-" + kind.name().toLowerCase());
            item.setDisable(!isAllowed(node, kind));
            item.setOnAction(event -> {
                if (newAction != null) {
                    newAction.accept(new ExplorerNewRequest(node, kind));
                }
            });
            newMenu.getItems().add(item);
        }
        menu.getItems().addAll(
                item("Open"),
                actionItem("Rename", node, renameAction),
                actionItem("Delete", node, deleteAction),
                new SeparatorMenuItem(),
                item("Copy Path"),
                item("Reveal"),
                new SeparatorMenuItem(),
                newMenu
        );
        return menu;
    }

    private static MenuItem actionItem(String text, ProjectNode node, Consumer<ProjectNode> action) {
        MenuItem menuItem = item(text);
        menuItem.setDisable(node == null);
        menuItem.setOnAction(event -> {
            if (action != null) action.accept(node);
        });
        return menuItem;
    }

    private static boolean isAllowed(ProjectNode node, ExplorerNewKind kind) {
        if (node == null) {
            return false;
        }
        Path location = node.type() == ProjectNodeType.FILE ? node.path().getParent() : node.path();
        return isJavaPath(location);
    }

    private static boolean isJavaPath(Path path) {
        if (path == null) {
            return false;
        }
        Path normalized = path.toAbsolutePath().normalize();
        if (hasJavaSourceMarker(normalized, "main") || hasJavaSourceMarker(normalized, "test")) {
            return true;
        }
        if (!"src".equalsIgnoreCase(normalized.getFileName().toString())) {
            return false;
        }
        return !Files.isDirectory(normalized.resolve("main"))
                && !Files.isDirectory(normalized.resolve("test"));
    }

    private static boolean hasJavaSourceMarker(Path path, String phase) {
        for (int i = 0; i + 2 < path.getNameCount(); i++) {
            if ("src".equalsIgnoreCase(path.getName(i).toString())
                    && phase.equalsIgnoreCase(path.getName(i + 1).toString())
                    && "java".equalsIgnoreCase(path.getName(i + 2).toString())) {
                return true;
            }
        }
        return false;
    }

    private static String label(ExplorerNewKind kind) {
        return switch (kind) {
            case PACKAGE -> "Package";
            case JAVA_CLASS -> "Java Class";
            case INTERFACE -> "Interface";
            case ENUM -> "Enum";
            case RECORD -> "Record";
            case MAIN_CLASS -> "Main Class";
            case JAVA_FILE -> "Java File";
        };
    }

    private static MenuItem item(String text) {
        MenuItem menuItem = new MenuItem(text);
        menuItem.setId("explorer-action-" + text.toLowerCase().replace(' ', '-'));
        return menuItem;
    }

    private static MenuItem disabledItem(String text) {
        MenuItem menuItem = item(text);
        menuItem.setDisable(true);
        return menuItem;
    }
}
