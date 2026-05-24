package com.eyecode.ui;

import javax.swing.*;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class TerminalPanel extends JPanel {


    private JTextPane terminalArea;

    private int promptPosition;

    private File currentDirectory;

    private final List<String> commandHistory = new ArrayList<>();

    private int historyIndex = -1;

    private Process shellProcess;
    private BufferedWriter shellInput;
    private BufferedReader shellOutput;

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

        startShell();


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

                    e.consume();

                    if (commandHistory.isEmpty()) return;

                    historyIndex--;

                    if (historyIndex < 0) historyIndex = 0;

                    replaceCurrentCommand(commandHistory.get(historyIndex));

                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {

                    e.consume();

                    if (commandHistory.isEmpty()) return;

                    historyIndex++;

                    if (historyIndex >= commandHistory.size()) {
                        historyIndex = commandHistory.size();

                        replaceCurrentCommand("");

                        return;
                    }

                    replaceCurrentCommand(commandHistory.get(historyIndex));

                    return;
                }

                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
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

//    private void appendPrompt(){
//
//        appendSeparator();
//
//        appendText(
//                "PS " + currentDirectory.getAbsolutePath() + "> "
//        );
//
//        promptPosition = terminalArea.getDocument().getLength();
//
//        moveCaretToEnd();
//
//    }

    /**
     * Processa o comando digitado.
     */
    private void handleCommand(){

        try {

            int length = terminalArea.getDocument().getLength();

            String command = terminalArea.getText(promptPosition, length - promptPosition).trim();

            if (command.isBlank()) {
                return;
            }

            commandHistory.add(command);
            historyIndex = commandHistory.size();

            appendSeparator();

            executeCommand(command);

        } catch (Exception e) {
            e.printStackTrace();
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

    private void replaceCurrentCommand(String text) {

        try {

            StyledDocument doc = terminalArea.getStyledDocument();

            doc.remove(promptPosition, doc.getLength() - promptPosition);

            doc.insertString(doc.getLength(), text, null);

            moveCaretToEnd();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void ui(Runnable runnable) {

        SwingUtilities.invokeLater(runnable);
    }

    private void appendColoredText(String text, Color color) {

        StyledDocument doc = terminalArea.getStyledDocument();

        Style style = terminalArea.addStyle("ColorStyle", null);

        StyleConstants.setForeground(style, color);

        try {
            doc.insertString(doc.getLength(), text, style);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void appendAnsiText(String text) {

        if (text.contains("\u001B[31m")) {

            text = text.replace("\u001B[31m", "");

            appendColoredText(text, new Color(255, 85, 85));

            return;
        }

        if (text.contains("\u001B[32m")) {

            text = text.replace("\u001B[32m", "");

            appendColoredText(text, new Color(80, 250, 123));

            return;
        }

        appendText(text);
    }

    private void startShell() {

        try {

            ProcessBuilder builder = new ProcessBuilder("powershell");

            builder.directory(currentDirectory);

            builder.redirectErrorStream(true);

            shellProcess = builder.start();

            shellInput = new BufferedWriter(new OutputStreamWriter(shellProcess.getOutputStream()));

            shellOutput = new BufferedReader(new InputStreamReader(shellProcess.getInputStream()));

            startOutputReader();


        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void startOutputReader() {

        new Thread(() -> {

            try {

                String line;

                while ((line = shellOutput.readLine()) != null) {

                    String finalLine = line;

                    ui(() -> {

                        appendAnsiText(finalLine + "\n");

                        moveCaretToEnd();
                    });
                }

                } catch (Exception e) {
                    e.printStackTrace();
            }


        }).start();
    }

    private void executeCommand(String command) {

        try {

            shellInput.write(command);

            shellInput.newLine();

            shellInput.flush();
        } catch (Exception e) {

            e.printStackTrace();
        }

        }
    }

