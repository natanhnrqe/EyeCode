package ide.java.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.File;


public class FileTreeCellRender extends DefaultTreeCellRenderer {

    private Icon folderIcon;

    private Icon fileIcon;

    private Icon javaIcon;

    public FileTreeCellRender() {
        folderIcon = UIManager.getIcon("FileView.directoryIcon");
        fileIcon = UIManager.getIcon("FileView.fileIcon");

        javaIcon = fileIcon;

    }
    @Override
    public Component getTreeCellRendererComponent(
            JTree jTree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus){

        super.getTreeCellRendererComponent(
                jTree, value, selected, expanded, leaf, row, hasFocus
        );

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;

        Object obj = node.getUserObject();

        if (obj instanceof File file){
            setText(file.getName().isEmpty() ? file.getAbsolutePath() : file.getName());

            if (file.isDirectory()){
                setIcon(folderIcon);
            }else {
                if (file.getName().endsWith(".java")){
                    setIcon(javaIcon);
                }else {
                    setIcon(fileIcon);
                }
            }
        }

    return this;
    }
}
