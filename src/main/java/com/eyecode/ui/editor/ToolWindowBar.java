package com.eyecode.ui.editor;

import com.eyecode.ui.ToolWindowButton;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.UIConstants;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ToolWindowBar extends JPanel {

    private Consumer<String> actionListener;

    private ToolWindowButton projectButton;
    private ToolWindowButton runButton;
    private ToolWindowButton terminalButton;
    private ToolWindowButton problemButton;
    private ToolWindowButton gitButton;

    public ToolWindowBar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(UIConstants.TOOLWINDOW_WIDTH, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(SpacingSystem.MD, 0, SpacingSystem.MD, 0));

        projectButton = createButton(IconManager.project(), "PROJECT", "Project");
        runButton = createButton(IconManager.run(), "RUN", "Run");
        terminalButton = createButton(IconManager.terminal(), "TERMINAL", "Terminal");
        problemButton = createButton(IconManager.problem(), "PROBLEM", "Problems");
        gitButton     = createButton(IconManager.git(), "GIT", "Git");

        add(projectButton);

        add(Box.createVerticalGlue());

        add(runButton);
        add(Box.createVerticalStrut(6));
        add(terminalButton);
        add(Box.createVerticalStrut(6));
        add(problemButton);
        add(Box.createVerticalStrut(6));
        add(gitButton);
    }

    public void setActionListener(Consumer<String> actionListener) {
        this.actionListener = actionListener;
    }

    private ToolWindowButton createButton(Icon icon, String action, String tooltip) {
        ToolWindowButton button = new ToolWindowButton(icon, tooltip);
        button.setToolTipText(tooltip);
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        button.addActionListener(e -> {
            selectedButton(button);

            if (actionListener != null) {
                actionListener.accept(action);
            }
        });

        return button;
    }

    private void selectedButton(ToolWindowButton button) {
        projectButton.setSelectedState(false);
        runButton.setSelectedState(false);
        terminalButton.setSelectedState(false);
        problemButton.setSelectedState(false);
        gitButton.setSelectedState(false);

        button.setSelectedState(true);
    }
}
