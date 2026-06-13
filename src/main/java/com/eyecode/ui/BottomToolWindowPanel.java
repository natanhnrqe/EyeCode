package com.eyecode.ui;

import com.eyecode.terminal.TerminalPanel;
import com.eyecode.ui.scroll.ModernScrollBarUI;

import javax.swing.*;
import java.awt.*;

public class BottomToolWindowPanel extends RoundedPanel {

    private JTabbedPane tabs;
    private JTextArea runOutputArea;
    private TerminalPanel terminalPanel;
    private JPanel runPanel;
    private JLabel runStatusLabel;

    public BottomToolWindowPanel() {
        setLayout(new BorderLayout());
        setBackground(new Color(25, 26, 28));

        tabs = new JTabbedPane();
        tabs.putClientProperty("JTabbedPane.tabHeight", 32);
        tabs.setOpaque(false);

        terminalPanel = new TerminalPanel();
        runPanel = new JPanel(new BorderLayout());

        tabs.addTab("Terminal", terminalPanel);
        tabs.addTab("Run", createRunPanel());

        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createRunPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(25, 26, 28));
        header.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 6));

        runStatusLabel = new JLabel("Run");
        runStatusLabel.setForeground(new Color(187, 187, 187));
        runStatusLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));

        JButton clearButton = new JButton(UiIcons.clear());
        clearButton.setToolTipText("Clear output");
        clearButton.setForeground(new Color(187, 187, 187));
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> clearRunOutput());

        header.add(runStatusLabel, BorderLayout.WEST);
        header.add(clearButton, BorderLayout.EAST);

        runOutputArea = new JTextArea();
        runOutputArea.setEditable(false);
        runOutputArea.setBackground(new Color(25, 26, 28));
        runOutputArea.setForeground(new Color(187, 187, 187));
        runOutputArea.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        runOutputArea.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));

        JScrollPane scrollPane = new JScrollPane(runOutputArea);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(48, 51, 57)));
        scrollPane.getVerticalScrollBar().setUI(new ModernScrollBarUI());
        scrollPane.getHorizontalScrollBar().setUI(new ModernScrollBarUI());

        runPanel.add(header, BorderLayout.NORTH);
        runPanel.add(scrollPane, BorderLayout.CENTER);

        return runPanel;
    }

    public void printRunOutput(String text) {
        runOutputArea.append(text + "\n");
        runOutputArea.setCaretPosition(runOutputArea.getDocument().getLength());
    }

    public void clearRunOutput() {
        runOutputArea.setText("");
    }

    public void setRunStatus(String status) {
        runStatusLabel.setText(status == null || status.isBlank() ? "Run" : status);
    }

    public void showTerminal() {
        setVisible(true);
        tabs.setSelectedIndex(0);
    }

    public void showRun() {
        setVisible(true);
        tabs.setSelectedIndex(1);
    }

    public void toggleVisibility() {
        setVisible(!isVisible());
    }

    public boolean isTerminalSelected() {
        return tabs.getSelectedIndex() == 0;
    }

    public boolean isRunSelected() {
        return tabs.getSelectedIndex() == 1;
    }
}
