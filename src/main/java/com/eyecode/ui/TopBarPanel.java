package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class TopBarPanel extends JPanel {

    public TopBarPanel() {

        setLayout(new BorderLayout());

        setPreferredSize(new Dimension(0, 42));

        setBackground(new Color(37, 37, 38));

        buildUi();
    }

    private void buildUi(){

       JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10 , 6));

        left.setOpaque(false);

        JLabel logo = new JLabel("EyeCode");

        logo.setFont(new Font("JetBrains Mono", Font.BOLD, 16));

        logo.setForeground(new Color(220, 220, 220));

        JLabel branch = new JLabel("main");

        branch.setForeground(new Color(150, 150, 150));


        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 5));

        JButton runButton = createToolbarButton("▶");
        JButton searchButton = createToolbarButton("⌕");
        JButton saveButton = createToolbarButton("💾");

        right.add(runButton);
        right.add(saveButton);
        right.add(searchButton);

        add(left, BorderLayout.WEST);

        add(right, BorderLayout.EAST);

    }

    private JButton createToolbarButton(String text) {

        JButton button = new JButton(text);

        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setContentAreaFilled(false);

        button.setForeground(new Color(220, 220 ,220));

        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        button.setFont(new Font("Dialog", Font.PLAIN, 16));

        button.setPreferredSize(new Dimension(36, 30));

        return button;
    }
}
