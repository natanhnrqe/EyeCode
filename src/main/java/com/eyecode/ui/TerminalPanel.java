package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;

public class TerminalPanel extends JPanel {

    /**
     * Area principal do terminal
     */
    private JTextArea terminalArea;

    private int promptPosition;

    private File currentDirectory;

    public TerminalPanel() {

        setLayout(new BorderLayout());

        terminalArea = new JTextArea();

        currentDirectory = new File(System.getProperty("user.dir"));

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


                int caretPosition = terminalArea.getCaretPosition();

                if (e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {

                    if (caretPosition <= promptPosition) {

                        e.consume();

                        return;
                    }
                }

                if (e.getKeyCode() == KeyEvent.VK_UP) {

                    if (caretPosition <= promptPosition) {
                        e.consume();
                    }
                }

                if (e.getKeyCode() == KeyEvent.VK_ENTER) {

                    e.consume();

                    handleCommand();
                }
            }
        });
        }

    private void appendPrompt(){
        terminalArea.append(
                "\nPS " + currentDirectory.getAbsolutePath() + "> "
        );

        promptPosition = terminalArea.getDocument().getLength();

        moveCaretToEnd();

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

            if (command.isBlank()) {
                appendPrompt();
                return;
            }

            if (command.startsWith("cd ")) {

                String path = command.substring(3).trim();

                File newDir = new File(currentDirectory, path);

                if (newDir.exists() && newDir.isDirectory()) {

                    currentDirectory = newDir.getCanonicalFile();

                } else {
                    terminalArea.append("\nDirectory not found");
                }

                appendPrompt();
                return;

            }

            ProcessBuilder builder = new ProcessBuilder(
                    "powershell",
                    "-Command",
                    command
            );

            builder.directory(currentDirectory);

            builder.redirectErrorStream(true);

            Process process = builder.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            process.getInputStream()
                    )
            );

            String line;

            terminalArea.append("\n");

            while ((line = reader.readLine()) != null) {

                terminalArea.append(line + "\n");
            }

            process.waitFor();

            appendPrompt();

        } catch (Exception e) {
            terminalArea.append("/nError: " + e.getMessage());

            appendPrompt();
        }
    }

    public void print(String text){
        terminalArea.append(text + "\n");
    }

    private void moveCaretToEnd(){

        terminalArea.setCaretPosition(
                terminalArea.getDocument().getLength()
        );
    }
}
