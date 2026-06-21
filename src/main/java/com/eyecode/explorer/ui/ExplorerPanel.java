package com.eyecode.explorer.ui;

import com.eyecode.explorer.model.ExplorerTree;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import java.awt.BorderLayout;

public class ExplorerPanel extends JPanel {

    private final ExplorerTreeModelAdapter modelAdapter;
    private final JTree tree;
    private final JScrollPane scrollPane;

    public ExplorerPanel(ExplorerTree explorerTree) {
        super(new BorderLayout());
        this.modelAdapter = new ExplorerTreeModelAdapter();
        this.tree = new JTree();
        this.scrollPane = new JScrollPane(tree);

        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new ExplorerTreeCellRenderer());

        add(scrollPane, BorderLayout.CENTER);
        setExplorerTree(explorerTree);
    }

    public void setExplorerTree(ExplorerTree explorerTree) {
        tree.setModel(modelAdapter.buildModel(explorerTree));
        tree.expandRow(0);
    }
}
