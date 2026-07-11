package com.eyecode.learning.ui;

import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

public final class LearningHoverPopup {

    private JWindow window;
    private final JLabel titleLabel;
    private final JLabel descriptionLabel;
    private final JLabel difficultyLabel;

    public LearningHoverPopup() {
        titleLabel = new JLabel();
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 13f));
        titleLabel.setForeground(ColorManager.TEXT_PRIMARY);

        descriptionLabel = new JLabel();
        descriptionLabel.setFont(descriptionLabel.getFont().deriveFont(11f));
        descriptionLabel.setForeground(ColorManager.TEXT_SECONDARY);

        difficultyLabel = new JLabel();
        difficultyLabel.setFont(difficultyLabel.getFont().deriveFont(10f));
    }

    public void show(Component owner, LearningConcept concept, Point screenLocation) {
        ensureWindow(owner);

        titleLabel.setText(concept.getTitle());
        descriptionLabel.setText(concept.getDescription());

        DifficultyLevel difficulty = concept.getDifficulty();
        difficultyLabel.setText("Difficulty: " + difficulty.name());
        switch (difficulty) {
            case BEGINNER -> difficultyLabel.setForeground(ColorManager.SUCCESS_GREEN);
            case INTERMEDIATE -> difficultyLabel.setForeground(ColorManager.STATUS_BUSY);
            case ADVANCED -> difficultyLabel.setForeground(ColorManager.ERROR_RED);
        }

        window.pack();
        window.setLocation(screenLocation.x + 12, screenLocation.y + 12);
        window.setVisible(true);
    }

    public void hide() {
        if (window != null) {
            window.setVisible(false);
        }
    }

    public boolean isVisible() {
        return window != null && window.isVisible();
    }

    private void ensureWindow(Component owner) {
        if (window != null) return;

        Window win = javax.swing.SwingUtilities.getWindowAncestor(owner);
        window = new JWindow(win);
        window.setFocusableWindowState(false);
        window.setBackground(new Color(0, 0, 0, 0));

        JPanel content = new JPanel(new GridBagLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(ColorManager.CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(ColorManager.BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(10, 12, 10, 12));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets = new Insets(0, 0, 4, 0);
        content.add(titleLabel, gbc);

        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 6, 0);
        content.add(descriptionLabel, gbc);

        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 0, 0);
        content.add(difficultyLabel, gbc);

        window.getContentPane().setLayout(new BorderLayout());
        window.getContentPane().setBackground(ColorManager.CARD_BG);
        window.getContentPane().add(content, BorderLayout.CENTER);
    }
}
