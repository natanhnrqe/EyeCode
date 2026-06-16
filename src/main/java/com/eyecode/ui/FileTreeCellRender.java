package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.io.File;

public class FileTreeCellRender extends DefaultTreeCellRenderer {

    public FileTreeCellRender() {
        setBackgroundNonSelectionColor(ColorManager.PANEL_BG);
        setTextNonSelectionColor(ColorManager.TEXT_FILE_TREE);
        setBackgroundSelectionColor(ColorManager.ACCENT_SELECTION);
        setTextSelectionColor(Color.WHITE);
    }

    private boolean selected;

    @Override
    public Color getBackgroundSelectionColor() {
        return painting ? null : ColorManager.ACCENT_BLUE;
    }

    private boolean painting;

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
            boolean hasFocus) {

        super.getTreeCellRendererComponent(jTree, value, selected, expanded, leaf, row, hasFocus);

        this.selected = selected;

        setIconTextGap(SpacingSystem.SM);
        setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS, SpacingSystem.XXS));

        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        Object obj = node.getUserObject();

        if (obj instanceof File file) {
            setText(file.getName().isEmpty() ? file.getAbsolutePath() : file.getName());

            if (file.isDirectory()) {
                boolean isRoot = (jTree.getModel().getRoot() == value);
                if (isRoot) {
                    setIcon(IconManager.projectDirectory());
                } else {
                    String name = file.getName();
                    if (name.equalsIgnoreCase("assets") || name.equalsIgnoreCase("resources")) {
                        setIcon(IconManager.assets());
                    } else {
                        setIcon(IconManager.folder());
                    }
                }
            } else {
                setIcon(IconManager.forFile(file.getName()));
            }
        }

        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XS, SpacingSystem.MD, SpacingSystem.XS, SpacingSystem.MD));
        setFont(TypographyManager.UI_TREE());

        return this;
    }
}
