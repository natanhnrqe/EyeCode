package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import java.awt.*;
import java.io.File;

public class BreadcrumpPanel extends JPanel {

    private JLabel pathLabel;

    public BreadcrumpPanel() {

        setLayout(new FlowLayout(FlowLayout.LEFT, SpacingSystem.MD, 6));
        setBackground(ColorManager.BREADCRUMB_BG);

        pathLabel = new JLabel();

        pathLabel.setForeground(ColorManager.TEXT_SECONDARY);
        pathLabel.setFont(TypographyManager.UI_PATH());

        add(pathLabel);
    }

    public void updatePath(File file) {
        if (file == null) {
            pathLabel.setText("");
            return;
        }
        String path = file.getPath().replace("\\", " > ");
        pathLabel.setText(path);
    }


}
