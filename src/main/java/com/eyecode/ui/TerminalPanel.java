package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class TerminalPanel extends JPanel {

    /**
     * Area principal do terminal
     */
    private JTextArea terminalArea;

    private int promptPosition;

    public TerminalPanel() {

        setLayout(new BorderLayout());

        terminalArea = new JTextArea();

        /**
         * Customizacao CONSOLE
         */

        terminalArea.setBackground(new Color(30, 30, 30));

        terminalArea.setForeground(new Color(220, 220, 220));

        terminalArea.setCaretColor(Color.WHITE);

        terminalArea.setFont(new Font("Consolas", Font.PLAIN, 14));

        terminalArea.setBorder(BorderFactory.createEmptyBorder(
                10,
                10,
                10,
                10
        ));

        JScrollPane scrollPane = new JScrollPane(terminalArea);

        add(scrollPane, BorderLayout.CENTER);

        appendPrompt();

        /**
         * Captura ENTER.
         */
        terminalArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    e.consume();

                    handleCommand();
                }
            }
        });
        }

    private void appendPrompt(){
        terminalArea.append(
                "\nPS EyeCode> "
        );

        promptPosition = terminalArea.getDocument().getLength();

    }

    /**
     * Processa o comando digitado.
     */
    private void handleCommand(){

        try {

            int length = terminalArea.getDocument().getLength();

            String command = terminalArea.getText(promptPosition,
                    length - promptPosition)
                    .trim();

            terminalArea.append("\nExecuted: " + command);

            appendPrompt();

        } catch (Exception e) {
            terminalArea.append("/nError: " + e.getMessage());

            appendPrompt();
        }
    }

    public void print(String text){
        terminalArea.append(text + "\n");
    }
}
