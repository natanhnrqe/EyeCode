package com.eyecode.javafx.explorer;

import com.eyecode.designsystem.icon.EyeCodeIcon;
import javafx.scene.control.TreeItem;

import java.nio.file.Path;

public record ExplorerRowViewModel(
        String title,
        EyeCodeIcon icon,
        int level,
        boolean expanded,
        boolean selected,
        boolean hasChildren,
        String badge,
        String status) {

    public static ExplorerRowViewModel from(TreeItem<ProjectNode> item, boolean selected) {
        ProjectNode node = item.getValue();
        boolean underJavaSource = underJavaSource(item);
        return new ExplorerRowViewModel(
                node.name(),
                ExplorerIconResolver.forNode(node, underJavaSource),
                level(item),
                item.isExpanded(),
                selected,
                !item.isLeaf(),
                null,
                null);
    }

    private static int level(TreeItem<ProjectNode> item) {
        int depth = 0;
        for (TreeItem<ProjectNode> parent = item.getParent(); parent != null; parent = parent.getParent()) {
            depth++;
        }
        return depth;
    }

    private static boolean underJavaSource(TreeItem<ProjectNode> item) {
        for (TreeItem<ProjectNode> parent = item.getParent(); parent != null; parent = parent.getParent()) {
            ProjectNode value = parent.getValue();
            if (value != null && hasJavaSegment(value.path())) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasJavaSegment(Path path) {
        if (path == null) {
            return false;
        }
        for (Path segment : path) {
            if ("java".equals(segment.toString())) {
                return true;
            }
        }
        return false;
    }
}
