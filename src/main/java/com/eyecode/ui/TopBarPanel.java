package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import com.eyecode.ui.designsystem.UIConstants;
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
        setPreferredSize(new Dimension(0, UIConstants.TOOLBAR_HEIGHT));
        setBackground(ColorManager.TOOLBAR_BG);
        setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, ColorManager.BORDER));
        buildUi();
    }

    private void buildUi() {
        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, SpacingSystem.LG, SpacingSystem.XS));
        left.setOpaque(false);

        JLabel logo = new JLabel("EyeCode");
        logo.setFont(TypographyManager.UI_LOGO());
        logo.setForeground(ColorManager.TEXT_PRIMARY);

        projectLabel = new JLabel("No project");
        projectLabel.setFont(TypographyManager.UI_LABEL());
        projectLabel.setForeground(ColorManager.TEXT_TERTIARY);

        left.add(logo);
        left.add(projectLabel);

        JPanel titleSpace = new JPanel(new BorderLayout());
        titleSpace.setOpaque(false);

        JPanel ideActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, SpacingSystem.XS, 3));
        ideActions.setOpaque(false);

        JButton newFileButton = createToolbarButton(IconManager.newFile(), "New file");
        JButton openFolderButton = createToolbarButton(IconManager.folder(), "Open folder");
        JButton saveButton = createToolbarButton(IconManager.save(), "Save file");
        runButton = createToolbarButton(IconManager.run(), "Run project");
        JButton settingsButton = createToolbarButton(IconManager.settings(), "Settings");

        ideActions.add(newFileButton);
        ideActions.add(openFolderButton);
        ideActions.add(saveButton);
        ideActions.add(runButton);
        ideActions.add(settingsButton);

        JPanel windowControls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        windowControls.setOpaque(false);
        windowControls.add(createWindowButton(IconManager.minimize(), "Minimize", false, () -> run(onMinimize)));
        windowControls.add(createWindowButton(IconManager.maximize(), "Maximize", false, () -> run(onMaximize)));
        windowControls.add(createWindowButton(IconManager.close(), "Close", true, () -> run(onClose)));

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
        JButton button = createBaseButton(icon, tooltip, new Dimension(SpacingSystem.TOOLBAR_BTN_W, SpacingSystem.TOOLBAR_BTN_H));
        button.setContentAreaFilled(false);
        button.addChangeListener(e -> updateToolbarButtonState(button));
        return button;
    }

    private JButton createWindowButton(Icon icon, String tooltip, boolean close, Runnable action) {
        JButton button = createBaseButton(icon, tooltip, new Dimension(SpacingSystem.WINDOW_BTN_W, SpacingSystem.WINDOW_BTN_H));
        button.setContentAreaFilled(false);
        button.addActionListener(e -> action.run());

        button.addChangeListener(e -> {
            if (button.getModel().isRollover()) {
                button.setContentAreaFilled(true);
                button.setBackground(close ? ColorManager.ERROR_RED : ColorManager.ACCENT_HOVER_BG);
                button.setForeground(Color.WHITE);
            } else {
                button.setContentAreaFilled(false);
                button.setForeground(ColorManager.TEXT_PRIMARY);
            }
        });

        return button;
    }

    private JButton createBaseButton(Icon icon, String tooltip, Dimension size) {
        JButton button = new JButton(icon);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setForeground(ColorManager.TEXT_PRIMARY);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setPreferredSize(size);
        button.setToolTipText(tooltip);
        return button;
    }

    private void updateToolbarButtonState(JButton button) {
        boolean activeRunButton = button == runButton && runActive;

        if (activeRunButton) {
            button.setContentAreaFilled(true);
            button.setBackground(ColorManager.SUCCESS_GREEN);
            button.setForeground(Color.WHITE);
            return;
        }

        if (button.getModel().isRollover()) {
            button.setContentAreaFilled(true);
            button.setBackground(ColorManager.ACCENT_HOVER_BG);
            button.setForeground(Color.WHITE);
            return;
        }

        button.setContentAreaFilled(false);
        button.setForeground(ColorManager.TEXT_PRIMARY);
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
