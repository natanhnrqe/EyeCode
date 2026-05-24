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


    private JTextPane outputPane;

    private JTextField inputField;

    private int promptPosition;

    private File currentDirectory;

    private final List<String> commandHistory = new ArrayList<>();

    private int historyIndex = -1;

    private Process shellProcess;
    private BufferedWriter shellInput;
    private BufferedReader shellOutput;

    public TerminalPanel() {

        /**
         * Customizacao SAIDA terminal
         */

        outputPane = new JTextPane();

        outputPane.setHighlighter(null);

        outputPane.setEditable(false);


        outputPane.addCaretListener(e -> {

            if (outputPane.getCaretPosition() < promptPosition) {

                moveCaretToEnd();
            }
        });

        outputPane.setBackground(new Color(30, 30, 30));

        outputPane.setForeground(new Color(220, 220, 220));

        outputPane.setCaretColor(Color.WHITE);

        outputPane.setBorder(BorderFactory.createEmptyBorder(
                12,
                12,
                12,
                12
        ));

        inputField = new JTextField();

        inputField.setBackground(new Color(43, 43, 43));

        inputField.setForeground(new Color(220, 220, 220));

        inputField.setCaretColor(Color.WHITE);

        inputField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0,
                new Color(60, 63, 65)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));

        setLayout(new BorderLayout());

        JScrollPane scrollPane = new JScrollPane(outputPane);

        add(scrollPane, BorderLayout.CENTER);

        add(inputField, BorderLayout.SOUTH);

        startShell();

        inputField.addActionListener(e -> {

            String command = inputField.getText().trim();

            if (command.isBlank()) return;

            commandHistory.add(command);

            historyIndex = commandHistory.size();

            appendCommand(command);

            executeCommand(command);

            inputField.setText("");
        });

        inputField.addKeyListener(new KeyAdapter(){

            @Override
            public void keyPressed(KeyEvent e) {

                if (e.getKeyCode() == KeyEvent.VK_UP) {

                    if (commandHistory.isEmpty()) return;

                    historyIndex--;

                    if (historyIndex < 0) {
                        historyIndex = 0;
                    }

                    inputField.setText(commandHistory.get(historyIndex));
                }

                if (e.getKeyCode() == KeyEvent.VK_DOWN) {

                    if (commandHistory.isEmpty()) return;

                    historyIndex++;

                    if (historyIndex >= commandHistory.size()) {

                        historyIndex = commandHistory.size();

                        inputField.setText("");

                        return;
                    }

                    inputField.setText(commandHistory.get(historyIndex));
                }
            }
        });
    }

    private void appendCommand(String command){

        appendSeparator();

        appendColoredText("PS > " + command + "\n\n",
                new Color(120, 170, 255));

    }

    /**
     * Processa o comando digitado.
     */
    private void handleCommand(){

        try {

            int length = outputPane.getDocument().getLength();

            String command = outputPane.getText(promptPosition, length - promptPosition).trim();

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

        outputPane.setCaretPosition(
                outputPane.getDocument().getLength()
        );
    }

    private void appendText(String text){

        StyledDocument doc = outputPane.getStyledDocument();

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


        outputPane.insertComponent(separator);

        appendText("\n");

    }

    private void replaceCurrentCommand(String text) {

        try {

            StyledDocument doc = outputPane.getStyledDocument();

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

        StyledDocument doc = outputPane.getStyledDocument();

        Style style = outputPane.addStyle("ColorStyle", null);

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

