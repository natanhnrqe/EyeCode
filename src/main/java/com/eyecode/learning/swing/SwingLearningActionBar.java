package com.eyecode.learning.swing;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;

public final class SwingLearningActionBar extends JPanel {

    private final JButton openDocsButton;
    private final JButton explainButton;
    private final JButton copyButton;
    private final JButton relatedButton;

    public SwingLearningActionBar() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(4, 0, 4, 0));
        setAlignmentX(Component.LEFT_ALIGNMENT);

        openDocsButton = createButton("Open Documentation");
        explainButton = createButton("Explain More");
        copyButton = createButton("Copy Code");
        relatedButton = createButton("Related Concepts");

        add(openDocsButton);
        add(Box.createHorizontalStrut(6));
        add(explainButton);
        add(Box.createHorizontalStrut(6));
        add(copyButton);
        add(Box.createHorizontalStrut(6));
        add(relatedButton);
        add(Box.createHorizontalGlue());
    }

    public void setDocumentationAction(Runnable action) {
        wireAction(openDocsButton, action);
    }

    public void setExplainAction(Runnable action) {
        wireAction(explainButton, action);
        explainButton.setEnabled(action != null);
    }

    public void setCopyAction(Runnable action) {
        wireAction(copyButton, action);
        copyButton.setEnabled(action != null);
    }

    public void setRelatedAction(Runnable action) {
        wireAction(relatedButton, action);
        relatedButton.setEnabled(action != null);
    }

    public void setRelatedEnabled(boolean enabled) {
        relatedButton.setEnabled(enabled);
    }

    private void wireAction(JButton button, Runnable action) {
        for (java.awt.event.ActionListener al : button.getActionListeners()) {
            button.removeActionListener(al);
        }
        if (action != null) {
            button.addActionListener(e -> action.run());
        }
    }

    private JButton createButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(TypographyManager.UI_SMALL());
        btn.setForeground(ColorManager.TEXT_SECONDARY);
        btn.setBackground(ColorManager.SURFACE_BG);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btn.setFocusable(false);
        return btn;
    }
}
