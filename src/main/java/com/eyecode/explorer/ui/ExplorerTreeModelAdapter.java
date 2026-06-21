package com.eyecode.explorer.ui;

import com.eyecode.explorer.model.ExplorerNode;
import com.eyecode.explorer.model.ExplorerTree;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

public final class ExplorerTreeModelAdapter {

    public DefaultTreeModel buildModel(ExplorerTree tree) {
        return new DefaultTreeModel(convert(tree.getRoot()));
    }

    private DefaultMutableTreeNode convert(ExplorerNode explorerNode) {
        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(explorerNode);
        for (ExplorerNode child : explorerNode.getChildren()) {
            swingNode.add(convert(child));
        }
        return swingNode;
    }
}
