package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class TopBarPanel extends JPanel {

    private Runnable onRun;
    private Runnable onSave;
    private Runnable onOpenFolder;
    private Runnable onNewFile;
    private Runnable onSettings;
    private Runnable onMinimize;
    private Runnable onMaximize;
    private Runnable onClose;

    private JLabel projectLabel;
    private JButton runButton;
    private boolean runActive;

    public TopBarPanel() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(0, 36));
        setBackground(new Color(37, 37, 38));
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(55, 58, 64)));
        buildUi();
    }

    private void buildUi() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));
        left.setOpaque(false);

        JLabel logo = new JLabel("EyeCode");
        logo.setFont(new Font("JetBrains Mono", Font.BOLD, 14));
        logo.setForeground(new Color(220, 220, 220));

        projectLabel = new JLabel("No project");
        projectLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));
        projectLabel.setForeground(new Color(150, 155, 165));

        left.add(logo);
        left.add(projectLabel);

        JPanel titleSpace = new JPanel(new BorderLayout());
        titleSpace.setOpaque(false);

        JPanel ideActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 3));
        ideActions.setOpaque(false);

        JButton newFileButton = createToolbarButton(UiIcons.newFile(), "New file");
        JButton openFolderButton = createToolbarButton(UiIcons.folder(), "Open folder");
        JButton saveButton = createToolbarButton(UiIcons.save(), "Save file");
        runButton = createToolbarButton(UiIcons.run(), "Run project");
        JButton settingsButton = createToolbarButton(UiIcons.settings(), "Settings");

        ideActions.add(newFileButton);
        ideActions.add(openFolderButton);
        ideActions.add(saveButton);
        ideActions.add(runButton);
        ideActions.add(settingsButton);

        JPanel windowControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        windowControls.setOpaque(false);
        windowControls.add(createWindowButton(UiIcons.minimize(), "Minimize", false, () -> run(onMinimize)));
        windowControls.add(createWindowButton(UiIcons.maximize(), "Maximize", false, () -> run(onMaximize)));
        windowControls.add(createWindowButton(UiIcons.close(), "Close", true, () -> run(onClose)));

        JPanel right = new JPanel(new BorderLayout());
        right.setOpaque(false);
        right.add(ideActions, BorderLayout.WEST);
        right.add(windowControls, BorderLayout.EAST);

        add(left, BorderLayout.WEST);
        add(titleSpace, BorderLayout.CENTER);
        add(right, BorderLayout.EAST);

        runButton.addActionListener(e -> run(onRun));
        saveButton.addActionListener(e -> run(onSave));
        openFolderButton.addActionListener(e -> run(onOpenFolder));
        newFileButton.addActionListener(e -> run(onNewFile));
        settingsButton.addActionListener(e -> run(onSettings));
    }

    private JButton createToolbarButton(Icon icon, String tooltip) {
        JButton button = createBaseButton(icon, tooltip, new Dimension(32, 28));
        button.setContentAreaFilled(false);
        button.addChangeListener(e -> updateToolbarButtonState(button));
        return button;
    }

    private JButton createWindowButton(Icon icon, String tooltip, boolean close, Runnable action) {
        JButton button = createBaseButton(icon, tooltip, new Dimension(44, 36));
        button.setContentAreaFilled(false);
        button.addActionListener(e -> action.run());

        button.addChangeListener(e -> {
            if (button.getModel().isRollover()) {
                button.setContentAreaFilled(true);
                button.setBackground(close ? new Color(196, 43, 28) : new Color(58, 61, 67));
                button.setForeground(Color.WHITE);
            } else {
                button.setContentAreaFilled(false);
                button.setForeground(new Color(210, 210, 210));
            }
        });

        return button;
    }

    private JButton createBaseButton(Icon icon, String tooltip, Dimension size) {
        JButton button = new JButton(icon);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setForeground(new Color(210, 210, 210));
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(size);
        button.setToolTipText(tooltip);
        return button;
    }

    private void updateToolbarButtonState(JButton button) {
        boolean activeRunButton = button == runButton && runActive;

        if (activeRunButton) {
            button.setContentAreaFilled(true);
            button.setBackground(new Color(42, 130, 73));
            button.setForeground(Color.WHITE);
            return;
        }

        if (button.getModel().isRollover()) {
            button.setContentAreaFilled(true);
            button.setBackground(new Color(58, 61, 67));
            button.setForeground(Color.WHITE);
            return;
        }

        button.setContentAreaFilled(false);
        button.setForeground(new Color(210, 210, 210));
    }

    private void run(Runnable action) {
        if (action != null) {
            action.run();
        }
    }

    public void setProjectName(String projectName) {
        projectLabel.setText(projectName == null || projectName.isBlank() ? "No project" : projectName);
    }

    public void setRunActive(boolean runActive) {
        this.runActive = runActive;

        if (runButton != null) {
            updateToolbarButtonState(runButton);
        }
    }

    public void setOnRun(Runnable onRun) {
        this.onRun = onRun;
    }

    public void setOnSave(Runnable onSave) {
        this.onSave = onSave;
    }

    public void setOnOpenFolder(Runnable onOpenFolder) {
        this.onOpenFolder = onOpenFolder;
    }

    public void setOnNewFile(Runnable onNewFile) {
        this.onNewFile = onNewFile;
    }

    public void setOnSettings(Runnable onSettings) {
        this.onSettings = onSettings;
    }

    public void setOnMinimize(Runnable onMinimize) {
        this.onMinimize = onMinimize;
    }

    public void setOnMaximize(Runnable onMaximize) {
        this.onMaximize = onMaximize;
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }
}
