package com.eyecode.ui;

import com.eyecode.terminal.TerminalPanel;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
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
        setBackground(ColorManager.EDITOR_BG);

        tabs = new JTabbedPane();
        tabs.putClientProperty("JTabbedPane.tabHeight", 32);
        tabs.setOpaque(false);

        terminalPanel = new TerminalPanel();
        runPanel = new JPanel(new BorderLayout());

        tabs.addTab("Terminal", terminalPanel);
        tabs.addTab("Run", createRunPanel());

        setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XS, SpacingSystem.XS, SpacingSystem.XS, SpacingSystem.XS));
        add(tabs, BorderLayout.CENTER);
    }

    private JPanel createRunPanel() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(ColorManager.EDITOR_BG);
        header.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.XS, SpacingSystem.MD, SpacingSystem.XS, SpacingSystem.SM));

        runStatusLabel = new JLabel("Run");
        runStatusLabel.setForeground(ColorManager.TEXT_SECONDARY);
        runStatusLabel.setFont(TypographyManager.UI_LABEL());

        JButton clearButton = new JButton(IconManager.clear());
        clearButton.setToolTipText("Clear output");
        clearButton.setForeground(ColorManager.TEXT_SECONDARY);
        clearButton.setFocusPainted(false);
        clearButton.setBorderPainted(false);
        clearButton.setContentAreaFilled(false);
        clearButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        clearButton.addActionListener(e -> clearRunOutput());

        header.add(runStatusLabel, BorderLayout.WEST);
        header.add(clearButton, BorderLayout.EAST);

        runOutputArea = new JTextArea();
        runOutputArea.setEditable(false);
        runOutputArea.setBackground(ColorManager.EDITOR_BG);
        runOutputArea.setForeground(ColorManager.TEXT_SECONDARY);
        runOutputArea.setFont(TypographyManager.UI_RUN_OUTPUT());
        runOutputArea.setBorder(BorderFactory.createEmptyBorder(SpacingSystem.MD, SpacingSystem.LG, SpacingSystem.MD, SpacingSystem.LG));

        JScrollPane scrollPane = new JScrollPane(runOutputArea);
        scrollPane.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, ColorManager.BORDER_DIVIDER));
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
