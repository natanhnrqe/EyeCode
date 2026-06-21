package com.eyecode.explorer.ui;

import com.eyecode.explorer.model.ExplorerNode;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.Component;

public final class ExplorerTreeCellRenderer extends DefaultTreeCellRenderer {

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected,
                                                  boolean expanded, boolean leaf, int row,
                                                  boolean hasFocus) {
        Component component = super.getTreeCellRendererComponent(
                tree,
                value,
                selected,
                expanded,
                leaf,
                row,
                hasFocus
        );

        if (value instanceof DefaultMutableTreeNode treeNode
                && treeNode.getUserObject() instanceof ExplorerNode explorerNode) {
            setText(explorerNode.getName());
        }

        return component;
    }
}
