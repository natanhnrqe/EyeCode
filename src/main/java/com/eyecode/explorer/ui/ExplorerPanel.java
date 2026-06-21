package com.eyecode.explorer.ui;

import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.ExplorerFileOpenRequestedEvent;
import com.eyecode.eventbus.events.ExplorerNodeSelectedEvent;
import com.eyecode.explorer.model.ExplorerNode;
import com.eyecode.explorer.model.ExplorerNodeType;
import com.eyecode.explorer.model.ExplorerTree;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.SwingUtilities;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Optional;

public class ExplorerPanel extends JPanel {

    private final ExplorerTreeModelAdapter modelAdapter;
    private final JTree tree;
    private final JScrollPane scrollPane;
    private final EventBus eventBus;

    public ExplorerPanel(ExplorerTree explorerTree, EventBus eventBus) {
        super(new BorderLayout());
        this.modelAdapter = new ExplorerTreeModelAdapter();
        this.tree = new JTree();
        this.scrollPane = new JScrollPane(tree);
        this.eventBus = eventBus;

        tree.setRootVisible(true);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new ExplorerTreeCellRenderer());
        installInteractionHandlers();

        add(scrollPane, BorderLayout.CENTER);
        setExplorerTree(explorerTree);
    }

    public void setExplorerTree(ExplorerTree explorerTree) {
        tree.setModel(modelAdapter.buildModel(explorerTree));
        tree.expandRow(0);
    }

    private void installInteractionHandlers() {
        tree.addTreeSelectionListener(event -> selectedNode()
                .ifPresent(node -> eventBus.publish(new ExplorerNodeSelectedEvent(node))));

        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                if (!SwingUtilities.isLeftMouseButton(event) || event.getClickCount() != 2) return;

                nodeAt(event)
                        .filter(node -> node.getType() == ExplorerNodeType.FILE)
                        .ifPresent(node -> eventBus.publish(
                                new ExplorerFileOpenRequestedEvent(node.getPath())
                        ));
            }
        });
    }

    private Optional<ExplorerNode> selectedNode() {
        TreePath path = tree.getSelectionPath();
        return nodeFromPath(path);
    }

    private Optional<ExplorerNode> nodeAt(MouseEvent event) {
        TreePath path = tree.getPathForLocation(event.getX(), event.getY());
        return nodeFromPath(path);
    }

    private Optional<ExplorerNode> nodeFromPath(TreePath path) {
        if (path == null) return Optional.empty();
        Object component = path.getLastPathComponent();
        if (component instanceof DefaultMutableTreeNode treeNode
                && treeNode.getUserObject() instanceof ExplorerNode explorerNode) {
            return Optional.of(explorerNode);
        }
        return Optional.empty();
    }
}
