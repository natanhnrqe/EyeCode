package com.eyecode.ui;

import com.eyecode.terminal.TerminalPanel;

import javax.swing.*;
import java.awt.*;

public class BottomToolWindowPanel extends JPanel {

    private JTabbedPane tabs;

    private JTextArea runOutputArea;

    private TerminalPanel terminalPanel;

    private JPanel runPanel;


    public BottomToolWindowPanel() {

        setLayout(new BorderLayout());

        setBackground(new Color(43, 45, 48));

        tabs = new JTabbedPane();

        tabs.putClientProperty("JTabbedPane.tabHeight", 32);

        terminalPanel = new TerminalPanel();

        runPanel = new JPanel(new BorderLayout());

        tabs.addTab("Terminal", terminalPanel);

        tabs.addTab("Run", createRunPanel());

        setBorder(
                BorderFactory.createEmptyBorder(
                        4,
                        4,
                        4,
                        4
                )
        );

        add(tabs, BorderLayout.CENTER);

    }

    private JPanel createRunPanel() {

        runOutputArea = new JTextArea();

        runOutputArea.setEditable(false);

        runOutputArea.setBackground(new Color(30, 30, 30));

        runOutputArea.setForeground(new Color(187, 187, 187));


        runPanel.add(new JScrollPane(runOutputArea), BorderLayout.CENTER);

        return runPanel;
    }

    public void printRunOutput(String text) {

        runOutputArea.append(text + "\n");
    }

    public void showTerminal() {

        setVisible(true);
        tabs.setSelectedIndex(0);
    }

    public void showRun() {

        setVisible(true);
        tabs.setSelectedIndex(1);
    }

    public void toggleVisibility(){

        setVisible(!isVisible());
    }

    public boolean isTerminalSelected(){

        return tabs.getSelectedIndex() == 0;
    }

    public boolean isRunSelected() {

        return tabs.getSelectedIndex() == 1;
    }
}
