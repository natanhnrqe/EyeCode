package com.eyecode.learning.swing;

import com.eyecode.ui.designsystem.ColorManager;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public final class SwingLearningBody extends JScrollPane {

    private final JPanel contentPanel;

    public SwingLearningBody() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);

        setViewportView(contentPanel);
        setBorder(null);
        setOpaque(false);
        getViewport().setOpaque(false);
        getViewport().setBackground(ColorManager.CARD_BG);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
