package com.eyecode.ui.editor;

import com.eyecode.ui.ToolWindowButton;
import com.eyecode.ui.UiIcons;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ToolWindowBar extends JPanel {

    private Consumer<String> actionListener;

    private ToolWindowButton projectButton;
    private ToolWindowButton runButton;
    private ToolWindowButton terminalButton;
    private ToolWindowButton problemButton;

    public ToolWindowBar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(64, 0));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));


        projectButton = createButton(UiIcons.project(), "PROJECT", "Project");
        runButton = createButton(UiIcons.run(), "RUN", "Run");
        terminalButton = createButton(UiIcons.terminal(), "TERMINAL", "Terminal");
        problemButton = createButton(UiIcons.problem(), "PROBLEM", "Problems");

        add(projectButton);
        
        add(Box.createVerticalGlue());
        
        add(runButton);
        add(Box.createVerticalStrut(6));
        add(terminalButton);
        add(Box.createVerticalStrut(6));
        add(problemButton);
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

        button.setSelectedState(true);
    }
}
