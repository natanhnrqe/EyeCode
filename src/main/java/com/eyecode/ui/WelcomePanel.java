package com.eyecode.ui;

import javax.swing.*;
import java.awt.*;

public class WelcomePanel extends JPanel {

    public WelcomePanel(Runnable onNewFile, Runnable onOpenFolder, Runnable onRun) {
        setLayout(new GridBagLayout());
        setBackground(new Color(30, 31, 34));

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("EyeCode");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setFont(new Font("JetBrains Mono", Font.BOLD, 34));
        title.setForeground(new Color(235, 235, 235));

        JLabel subtitle = new JLabel("Abra um arquivo ou uma pasta para comecar.");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setFont(new Font("JetBrains Mono", Font.PLAIN, 14));
        subtitle.setForeground(new Color(150, 155, 165));

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        actions.add(createActionButton("New File", UiIcons.newFile(), onNewFile));
        actions.add(createActionButton("Open Folder", UiIcons.folder(), onOpenFolder));
        actions.add(createActionButton("Run Project", UiIcons.run(), onRun));

        content.add(title);
        content.add(Box.createVerticalStrut(8));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(22));
        content.add(actions);

        add(content);
    }

    private JButton createActionButton(String text, Icon icon, Runnable action) {
        JButton button = new JButton(text, icon);
        button.setFont(new Font("JetBrains Mono", Font.PLAIN, 13));
        button.setForeground(new Color(220, 220, 220));
        button.setBackground(new Color(49, 51, 56));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(67, 70, 76)),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(8);
        button.addActionListener(e -> action.run());
        return button;
    }
}
