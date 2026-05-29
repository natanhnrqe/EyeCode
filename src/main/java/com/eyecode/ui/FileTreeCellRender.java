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

    private boolean selected;

    private boolean hovered;

    public FileTreeCellRender() {

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

        this.selected = selected;

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

        setOpaque(false);

        setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        setFont(new Font("JetBrains Mono", Font.PLAIN, 13));

        if (selected) {

            setForeground(new Color(255, 255, 255));
        } else {

            setForeground(new Color(210, 210, 210));
        }


    return this;
    }

    private Icon loadIcon(String path, int size) {
        ImageIcon icon = new ImageIcon(getClass().getResource(path));
        Image image = icon.getImage();

        Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);

        return new ImageIcon(scaled);
    }

    @Override
    protected void paintComponent(Graphics g) {

        Graphics2D g2 = (Graphics2D) g.create();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        if (selected) {
            g2.setColor(new Color(55, 57, 61));

            g2.fillRoundRect(
                    2,
                    2,
                    getWidth() - 4,
                    getHeight() - 4,
                    10,
                    10
            );
        }



        g2.dispose();

        super.paintComponent(g);
    }
}
