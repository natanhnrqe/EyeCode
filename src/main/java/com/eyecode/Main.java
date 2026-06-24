package com.eyecode;


import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
import com.formdev.flatlaf.FlatDarkLaf;
import com.eyecode.ui.MainWindow;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();

        UIManager.put("defaultFont", TypographyManager.UI_DEFAULT());
        UIManager.put("Tree.rowHeight", UIConstants.TREE_ROW_HEIGHT);
        UIManager.put("TabbedPane.tabInsets", new Insets(5,10,5,10));
        UIManager.put("Tree.paintLines", false);




        UIManager.put("Panel.background", ColorManager.PANEL_BG);
        UIManager.put("TabbedPane.background", ColorManager.PANEL_BG);
        UIManager.put("TabbedPane.selectedBackground", ColorManager.SELECTED_TAB_BG);
        UIManager.put("SplitPane.background", ColorManager.PANEL_BG);
        UIManager.put("Tree.background", ColorManager.PANEL_BG);
        UIManager.put("Viewport.background", ColorManager.PANEL_BG);
        UIManager.put("ScrollPane.background", ColorManager.PANEL_BG);
        UIManager.put("ToolBar.background", ColorManager.TOOLBAR_BG);
        UIManager.put("MenuBar.background", ColorManager.TOOLBAR_BG);
        UIManager.put("Menu.background", ColorManager.TOOLBAR_BG);
        UIManager.put("MenuItem.background", ColorManager.TOOLBAR_BG);

        UIManager.put("SplitPaneDivider.style", "plain");
        UIManager.put("SplitPaneDivider.gripColor", ColorManager.DIVIDER_COLOR);
        



        SwingUtilities.invokeLater(() -> {
            new MainWindow();

        });

    }

}
