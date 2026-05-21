package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class ConsolePanel extends JPanel {

    private JTextArea outputArea;
    private JTextField inputField;

    public ConsolePanel() {

        setLayout(new BorderLayout());

        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setBorder(
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        );

        JScrollPane scrollPane = new JScrollPane(outputArea);

        inputField = new JTextField();
        inputField.setToolTipText("Digite um comando...");
        inputField.setBorder(
                BorderFactory.createCompoundBorder(
                        BorderFactory.createLineBorder(new Color(60,63,65), 1),
                        BorderFactory.createEmptyBorder(6, 8, 6, 8)
                )
        );



        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);


    }

    public void print(String text){
        outputArea.append(text + "\n");
    }

    public JTextField getInputField() {
        return inputField;
    }
}
