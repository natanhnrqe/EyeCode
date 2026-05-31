package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class StatusBar extends JPanel {

    private JLabel languageLabel;
    private JLabel positionLabel;
    private JLabel statusLabel;

    public StatusBar() {

        setLayout(new BorderLayout());

        setPreferredSize(new Dimension(0, 26));

        setBackground(new Color(43, 43, 43));

        languageLabel = new JLabel("Java");
        positionLabel = new JLabel("Ln 1, Col 1");
        statusLabel = new JLabel("Ready");

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 4));

        left.setOpaque(false);
        left.add(languageLabel);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));

        right.setOpaque(false);
        right.add(positionLabel);
        right.add(statusLabel);

        add(left, BorderLayout.WEST);
        add(right, BorderLayout.EAST);
    }

    public void updateCaretPosition(int line, int columm) {

        positionLabel.setText("Ln " + line + ", Col " + columm);

    }
}
