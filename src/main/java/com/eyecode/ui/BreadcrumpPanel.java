package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;
import java.io.File;

public class BreadcrumpPanel extends JPanel {

    private JLabel pathLabel;

    public BreadcrumpPanel() {

        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 6));
        setBackground(new Color(43, 45, 48));

        pathLabel = new JLabel();

        pathLabel.setForeground(new Color(187, 187, 187));
        pathLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 12));

        add(pathLabel);
    }

    public void updatePath(File file) {

        if (file == null) {

            pathLabel.setText("");

            return;
        }

        String path = file.getPath();
        path = path.replace("\\", "> ");

        pathLabel.setText(path);
    }


}
