package com.eyecode.ui.editor;

import com.eyecode.ui.ToolWindowButton;

import javax.swing.*;
import javax.tools.Tool;
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

        add(Box.createVerticalStrut(8));

        projectButton =
                createButton("Pasta", "PROJECT");

        terminalButton =
                createButton(">_", "TERMINAL");

        runButton =
                createButton("▶", "RUN");

        add(projectButton);
        add(terminalButton);
        add(runButton);

        add(Box.createVerticalGlue());
    }

    public void setActionListener(Consumer<String> actionListener) {
        this.actionListener = actionListener;
    }

    private ToolWindowButton createButton(String text, String action) {

        ToolWindowButton button = new ToolWindowButton(text);

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
