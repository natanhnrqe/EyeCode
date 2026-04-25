package ide.java;

import com.formdev.flatlaf.FlatDarkLaf;
import ide.java.ui.MainWindow;

import javax.swing.*;
import java.awt.*;

public class Main {
    public static void main(String[] args) {
        FlatDarkLaf.setup();

        UIManager.put("defaultFont", new Font("JetBrains Mono", Font.PLAIN, 14));
        UIManager.put("Tree.rowHeight", 24);
        UIManager.put("TabbedPane.tabInsets", new Insets(5,10,5,10));

        SwingUtilities.invokeLater(() -> {
            new MainWindow();
        });

    }
}
