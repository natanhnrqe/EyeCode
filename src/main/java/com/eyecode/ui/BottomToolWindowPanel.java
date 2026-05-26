package com.eyecode.ui;

import com.eyecode.terminal.TerminalPanel;

import javax.swing.*;
import java.awt.*;

public class BottomToolWindowPanel extends JPanel {

    private JTabbedPane tabs;

    private JTextArea runOutputArea;

    public BottomToolWindowPanel() {

        setLayout(new BorderLayout());

        setBackground(new Color(43, 45, 48));

        tabs = new JTabbedPane();

        tabs.putClientProperty("JTabbedPane.tabHeight", 32);

        tabs.addTab("Terminal", new TerminalPanel());

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

        JPanel panel = new JPanel(new BorderLayout());

        runOutputArea = new JTextArea();

        runOutputArea.setEditable(false);

        runOutputArea.setBackground(new Color(30, 30, 30));

        runOutputArea.setForeground(new Color(187, 187, 187));



        panel.add(new JScrollPane(runOutputArea), BorderLayout.CENTER);

        return panel;
    }

    public void printRunOutput(String text) {

        runOutputArea.append(text + "\n");
    }
}
