package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
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

        folderIcon = loadIcon("/icons/pasta.png", SpacingSystem.TREE_ICON_SIZE);
        fileIcon   = loadIcon("/icons/arquivo.png", SpacingSystem.TREE_ICON_SIZE);
        javaIcon   = loadIcon("/icons/javaico.png", SpacingSystem.TREE_ICON_SIZE);

        setBackgroundNonSelectionColor(ColorManager.PANEL_BG);
        setTextNonSelectionColor(ColorManager.TEXT_FILE_TREE);
        setBackgroundSelectionColor(ColorManager.ACCENT_SELECTION);
        setTextSelectionColor(Color.WHITE);
    }

    private boolean selected;
    private boolean painting;

    @Override
    public Color getBackgroundSelectionColor() {
        return painting ? null : ColorManager.ACCENT_BLUE;
    }

    @Override
    protected void paintComponent(Graphics g) {
        if (selected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(ColorManager.ACCENT_BLUE);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
            g2.dispose();
        }
        painting = true;
        super.paintComponent(g);
        painting = false;
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

        setIconTextGap(SpacingSystem.SM);
        setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS));

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

        setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XS, SpacingSystem.MD, SpacingSystem.XS, SpacingSystem.MD));
        setFont(TypographyManager.UI_TREE());




    return this;
    }

    private Icon loadIcon(String path, int size) {
        ImageIcon icon = new ImageIcon(getClass().getResource(path));
        Image image = icon.getImage();

        Image scaled = image.getScaledInstance(size, size, Image.SCALE_SMOOTH);

        return new ImageIcon(scaled);
    }

}
