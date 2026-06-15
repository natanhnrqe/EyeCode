package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import javax.swing.*;
import java.awt.*;

public class ActivityBar extends JPanel {

    public ActivityBar() {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setPreferredSize(new Dimension(50, 0));
        setBackground(new Color(37, 37, 38));

        addButton(IconManager.folder(), "Project");
        addButton(IconManager.search(), "Search");
        addButton(IconManager.git(), "Version Control");
        addButton(IconManager.run(), "Run");
    }

    private void addButton(Icon icon, String tooltip) {
        JButton button = new JButton(icon);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setBackground(new Color(37, 37, 38));
        button.setForeground(ColorManager.TEXT_SECONDARY);
        button.setMaximumSize(new Dimension(50, 50));
        button.setPreferredSize(new Dimension(50, 50));
        add(button);
    }
}
