package com.eyecode.learning.swing;

import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.IconManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.event.ActionEvent;

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

        openDocsButton = createButton("Open Documentation",
                () -> System.out.println("Documentation action triggered"));
        explainButton = createButton("Explain More",
                () -> System.out.println("Explain action triggered"));
        copyButton = createButton("Copy Code",
                () -> System.out.println("Copy action triggered"));
        relatedButton = createButton("Related Concepts",
                () -> System.out.println("Related concepts action triggered"));

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
        for (java.awt.event.ActionListener al : openDocsButton.getActionListeners()) {
            openDocsButton.removeActionListener(al);
        }
        openDocsButton.addActionListener(e -> {
            if (action != null) action.run();
        });
    }

    private JButton createButton(String text, Runnable action) {
        JButton btn = new JButton(text);
        btn.setFont(TypographyManager.UI_SMALL());
        btn.setForeground(ColorManager.TEXT_SECONDARY);
        btn.setBackground(ColorManager.SURFACE_BG);
        btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(ColorManager.BORDER, 1),
                BorderFactory.createEmptyBorder(4, 8, 4, 8)
        ));
        btn.setFocusable(false);
        btn.addActionListener(e -> action.run());
        return btn;
    }
}
