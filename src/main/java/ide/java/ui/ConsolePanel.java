package ide.java.ui;

import javax.swing.*;
import java.awt.*;

public class ConsolePanel extends JPanel {

    private JTextArea consoleArea;

    public ConsolePanel() {

        setLayout(new BorderLayout());

        consoleArea = new JTextArea();
        consoleArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        consoleArea.setEditable(false);
        consoleArea.setBackground(Color.BLACK);
        consoleArea.setForeground(Color.GREEN);

        JScrollPane scrollPane = new JScrollPane(consoleArea);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void print(String text){
        consoleArea.append(text + "/n");
    }

}
