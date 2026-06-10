package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class StatusBar extends JPanel {

    private final JLabel pathLabel;
    private final JLabel languageLabel;
    private final JLabel positionLabel;
    private final JLabel statusLabel;
    private final JLabel fileLabel;
    private final JLabel projectLabel;

    public StatusBar() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 30));
        setBackground(new Color(24, 25, 28));
        setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(48, 51, 57)));

        pathLabel = createLabel("");
        pathLabel.setForeground(new Color(145, 150, 160));

        projectLabel = createLabel("EyeCode");
        fileLabel = createLabel("No file");
        languageLabel = createLabel("Plain Text");
        positionLabel = createLabel("Ln 1, Col 1");
        statusLabel = createLabel("Ready");

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
        left.setOpaque(false);
        left.add(pathLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 5));
        right.setOpaque(false);
        right.add(projectLabel);
        right.add(fileLabel);
        right.add(languageLabel);
        right.add(positionLabel);
        right.add(statusLabel);

        add(left, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);
    }

    public void updateCaretPosition(int line, int column) {
        positionLabel.setText("Ln " + line + ", Col " + column);
    }

    public void updateFile(String fileName) {
        fileLabel.setText(fileName == null || fileName.isBlank() ? "No file" : fileName);
        updateLanguage(fileName);
    }

    public void updatePath(File file) {
        if (file == null) {
            pathLabel.setText("");
            return;
        }

        pathLabel.setText(file.getPath().replace("\\", " > "));
    }

    public void updateProject(String projectName) {
        projectLabel.setText(projectName == null || projectName.isBlank() ? "EyeCode" : projectName);
    }

    public void updateStatus(String status) {
        statusLabel.setForeground(new Color(187, 187, 187));
        statusLabel.setText(status == null || status.isBlank() ? "Ready" : status);
    }

    public void setRunning(boolean running) {
        statusLabel.setText(running ? "Running" : "Ready");
        statusLabel.setForeground(running ? new Color(87, 166, 74) : new Color(187, 187, 187));
    }

    private void updateLanguage(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            languageLabel.setText("Plain Text");
            return;
        }

        String extension = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();
        languageLabel.setText(switch (extension) {
            case "java" -> "Java";
            case "xml" -> "XML";
            case "md" -> "Markdown";
            case "json" -> "JSON";
            default -> extension.toUpperCase();
        });
    }

    private JLabel createLabel(String text) {
        JLabel label = new JLabel(text);
        label.setForeground(new Color(187, 187, 187));
        label.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        return label;
    }
}
