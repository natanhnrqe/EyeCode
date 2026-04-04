package ide.java.ui;

import javax.swing.*;
import java.awt.*;

public class EditorPanel extends JPanel {

    private JTextArea textArea;

    public EditorPanel() {

        setLayout(new BorderLayout());

        textArea = new JTextArea();
        textArea.setFont(new Font("Consolas", Font.PLAIN, 14));
        textArea.setBackground(Color.DARK_GRAY);
        textArea.setForeground(Color.WHITE);

        JScrollPane scrollPane = new JScrollPane(textArea);

        add(scrollPane, BorderLayout.CENTER);

    }

    public JTextArea getTextArea() {
        return textArea;
    }
}
