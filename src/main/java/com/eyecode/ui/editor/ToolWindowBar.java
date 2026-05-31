package com.eyecode.ui.editor;

import javax.swing.*;
import java.awt.*;
import java.util.function.Consumer;

public class ToolWindowBar extends JPanel {

    private Consumer<String> actionListener;

    public ToolWindowBar() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(48, 0));

        setBackground(new Color(43, 43, 43));

        add(Box.createVerticalStrut(8));

        add(createButton("📁", "PROJECT"));
        add(createButton(">_", "TERMINAL"));
        add(createButton("▶", "RUN"));

        add(Box.createVerticalGlue());
    }

    public void setActionListener(Consumer<String> actionListener) {
        this.actionListener = actionListener;
    }

    private JButton createButton(String text, String action) {

        JButton button = new JButton(text);

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);
        button.setOpaque(false);

        button.setForeground(new Color(169, 183, 198));
        button.setMaximumSize(new Dimension(48, 40));
        button.setPreferredSize(new Dimension(48, 40));
        button.setAlignmentX(Component.CENTER_ALIGNMENT);

        button.addActionListener(e -> {

            if (actionListener != null) {
                actionListener.accept(action);
            }
        });

        return button;
    }
}
