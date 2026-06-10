package com.eyecode.ui.editor;

import com.eyecode.ui.ToolWindowButton;
import com.eyecode.ui.UiIcons;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ToolWindowBar extends JPanel {

    private Consumer<String> actionListener;

    private ToolWindowButton projectButton;
    private ToolWindowButton terminalButton;
    private ToolWindowButton runButton;

    public ToolWindowBar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(48, 0));
        setBackground(new Color(43, 43, 43));
        setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));

        projectButton = createButton(UiIcons.project(), "PROJECT", "Project");
        terminalButton = createButton(UiIcons.terminal(), "TERMINAL", "Terminal");
        runButton = createButton(UiIcons.run(), "RUN", "Run");

        add(projectButton);
        add(terminalButton);
        add(runButton);
        add(Box.createVerticalGlue());
    }

    public void setActionListener(Consumer<String> actionListener) {
        this.actionListener = actionListener;
    }

    private ToolWindowButton createButton(Icon icon, String action, String tooltip) {
        ToolWindowButton button = new ToolWindowButton(icon);
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

        button.setSelectedState(true);
    }
}
