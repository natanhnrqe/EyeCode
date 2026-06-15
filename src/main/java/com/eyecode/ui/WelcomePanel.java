package com.eyecode.ui;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.SpacingSystem;
import com.eyecode.ui.designsystem.TypographyManager;
import javax.swing.*;
import java.awt.*;

public class WelcomePanel extends JPanel {

    public WelcomePanel(Runnable onNewFile, Runnable onOpenFolder, Runnable onRun) {
        setLayout(new GridBagLayout());
        setBackground(ColorManager.PANEL_BG);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));

        JLabel title = new JLabel("EyeCode");
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        title.setFont(TypographyManager.UI_WELCOME());
        title.setForeground(ColorManager.TEXT_PRIMARY);

        JLabel subtitle = new JLabel("Abra um arquivo ou uma pasta para comecar.");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setFont(TypographyManager.UI_WELCOME_SUB());
        subtitle.setForeground(ColorManager.TEXT_TERTIARY);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, SpacingSystem.MD, 0));
        actions.setOpaque(false);
        actions.setAlignmentX(Component.LEFT_ALIGNMENT);

        actions.add(createActionButton("New File", IconManager.newFile(), onNewFile));
        actions.add(createActionButton("Open Folder", IconManager.folder(), onOpenFolder));
        actions.add(createActionButton("Run Project", IconManager.run(), onRun));

        content.add(title);
        content.add(Box.createVerticalStrut(SpacingSystem.MD));
        content.add(subtitle);
        content.add(Box.createVerticalStrut(SpacingSystem.XXXL));
        content.add(actions);

        add(content);
    }

    private JButton createActionButton(String text, Icon icon, Runnable action) {
        JButton button = new JButton(text, icon);
        button.setFont(TypographyManager.UI_BUTTON());
        button.setForeground(ColorManager.TEXT_PRIMARY);
        button.setBackground(ColorManager.CARD_BG);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER_CARD),
                BorderFactory.createEmptyBorder(SpacingSystem.MD, SpacingSystem.XL, SpacingSystem.MD, SpacingSystem.XL)
        ));
        button.setFocusPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setIconTextGap(SpacingSystem.MD);
        button.addActionListener(e -> action.run());
        return button;
    }
}
