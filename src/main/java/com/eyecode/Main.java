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
