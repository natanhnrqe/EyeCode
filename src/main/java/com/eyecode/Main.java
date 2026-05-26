package com.eyecode;

import com.eyecode.run.ProjectScanner;
import com.formdev.flatlaf.FlatDarkLaf;
import com.eyecode.ui.MainWindow;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();


        UIManager.put("defaultFont", loadEditorFont());
        UIManager.put("Tree.rowHeight", 24);
        UIManager.put("TabbedPane.tabInsets", new Insets(5,10,5,10));

        UIManager.put(
                "Panel.background",
                new Color(30, 30, 30)
        );

        UIManager.put(
                "TabbedPane.background",
                new Color(30, 30, 30)
        );

        UIManager.put(
                "TabbedPane.selectedBackground",
                new Color(37, 37, 38)
        );

        UIManager.put(
                "SplitPane.background",
                new Color(30, 30, 30)
        );

        UIManager.put(
                "Tree.background",
                new Color(30, 30, 30)
        );

        UIManager.put(
                "Viewport.background",
                new Color(30, 30, 30)
        );

        UIManager.put(
                "ScrollPane.background",
                new Color(30, 30, 30)
        );

        UIManager.put(
                "ToolBar.background",
                new Color(37, 37, 38)
        );

        UIManager.put(
                "MenuBar.background",
                new Color(37, 37, 38)
        );

        UIManager.put(
                "Menu.background",
                new Color(37, 37, 38)
        );

        UIManager.put(
                "MenuItem.background",
                new Color(37, 37, 38)
        );

        /**
         * Divider styling
         */
        UIManager.put(
                "SplitPaneDivider.style",
                "plain"
        );

        UIManager.put(
                "SplitPaneDivider.gripColor",
                new Color(60, 60, 60)
        );



        SwingUtilities.invokeLater(() -> {
            new MainWindow();

        });

    }

    private static Font loadEditorFont() {

        GraphicsEnvironment ge =
                GraphicsEnvironment
                        .getLocalGraphicsEnvironment();

        for (String name :
                ge.getAvailableFontFamilyNames()) {

            if (name.equalsIgnoreCase(
                    "JetBrains Mono")) {

                return new Font(
                        "JetBrains Mono",
                        Font.PLAIN,
                        13
                );
            }
        }

        return new Font(
                "Consolas",
                Font.PLAIN,
                13
        );
    }
}
