package com.eyecode.ui;

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
        folderIcon = new ImageIcon(getClass().getResource("/icons/pasta.png"));
        fileIcon = new ImageIcon(getClass().getResource("/icons/arquivo.png"));

        javaIcon = new ImageIcon(getClass().getResource("/icons/javaico.png"));

        folderIcon = loadIcon("/icons/pasta.png", 18);
        fileIcon   = loadIcon("/icons/arquivo.png", 18);
        javaIcon   = loadIcon("/icons/javaico.png", 18);

        setBackgroundNonSelectionColor(new Color(30,30,30));
        setTextNonSelectionColor(new Color(169, 183,198));
        setBackgroundSelectionColor(new Color(68,71,74));
        setTextSelectionColor(Color.WHITE);

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

        setIconTextGap(6);
        setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));

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

    private Icon loadIcon(String path, int size) {
        ImageIcon icon = new ImageIcon(getClass().getResource(path));
        Image image = icon.getImage();

        Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);

        return new ImageIcon(scaled);
    }
}
