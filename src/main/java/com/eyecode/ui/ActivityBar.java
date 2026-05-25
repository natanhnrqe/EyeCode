package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class ActivityBar extends JPanel {

    public ActivityBar() {

        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        setPreferredSize(new Dimension(50, 0));

        setBackground(new Color(37, 37, 38));

        addButton("📁");
        addButton("🔍");
        addButton("🌱");
        addButton("▶");
    }

    private void addButton(String text) {

        JButton button = new JButton(text);

        button.setFocusPainted(false);

        button.setBorderPainted(false);

        button.setBackground(new Color(37, 37, 38));
        button.setForeground(new Color(187, 187, 187));

        button.setMaximumSize(new Dimension(50, 50));

        button.setPreferredSize(new Dimension(50, 50));

        add(button);
    }
}
