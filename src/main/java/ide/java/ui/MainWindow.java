package ide.java.ui;

import javax.swing.*;
import java.awt.*;

public class MainWindow extends JFrame {

    private EditorPanel editorPanel;
    private ConsolePanel consolePanel;

    public MainWindow(){

        setTitle("Mini IDE");
        setSize(100, 700);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        editorPanel = new EditorPanel();
        consolePanel = new ConsolePanel();

        JSplitPane splitPane = new JSplitPane(
                JSplitPane.VERTICAL_SPLIT,
                editorPanel,
                consolePanel
        );

        splitPane.setDividerLocation(450);

        add(splitPane, BorderLayout.CENTER);

        setVisible(true);
    }
}
