package com.eyecode.ui;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
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
    private JTextPane terminalArea;

    private int promptPosition;

    private File currentDirectory;

    public TerminalPanel() {

        setLayout(new BorderLayout());

        terminalArea = new JTextPane();

        terminalArea.setHighlighter(null);

        currentDirectory = new File(System.getProperty("user.dir"));

        terminalArea.addCaretListener(e -> {

            if (terminalArea.getCaretPosition() < promptPosition) {

                moveCaretToEnd();
            }
        });

        /**
         * Customizacao CONSOLE
         */

        terminalArea.setBackground(new Color(30, 30, 30));

        terminalArea.setForeground(new Color(220, 220, 220));

        terminalArea.setCaretColor(Color.WHITE);



        terminalArea.setBorder(BorderFactory.createEmptyBorder(
                12,
                12,
                12,
                12
        ));

        terminalArea.setEditable(true);

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

                if (
                        e.getKeyCode() == KeyEvent.VK_DOWN ||
                        e.getKeyCode() == KeyEvent.VK_LEFT ||
                        e.getKeyCode() == KeyEvent.VK_UP
                ) {
                    if (caretPosition <= promptPosition) {
                        e.consume();
                        moveCaretToEnd();
                        return;
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

        appendSeparator();

        appendText(
                "PS " + currentDirectory.getAbsolutePath() + "> "
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
                    appendText("\nDirectory not found");
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

            appendText("\n");

            while ((line = reader.readLine()) != null) {

                appendText(line + "\n");
            }

            process.waitFor();

            appendPrompt();

        } catch (Exception e) {
            appendText("/nError: " + e.getMessage());

            appendPrompt();
        }
    }

    public void print(String text){
        appendText(text + "\n");
    }

    private void moveCaretToEnd(){

        terminalArea.setCaretPosition(
                terminalArea.getDocument().getLength()
        );
    }

    private void appendText(String text){

        StyledDocument doc = terminalArea.getStyledDocument();

        try {

            doc.insertString(
                    doc.getLength(),
                    text,
                    null
            );

        } catch (Exception e) {

            e.printStackTrace();
        }

    }

    private void appendSeparator() {

        JSeparator separator = new JSeparator();

        separator.setForeground(new Color(60, 63, 65));


        terminalArea.insertComponent(separator);

        appendText("\n");
    }
}
