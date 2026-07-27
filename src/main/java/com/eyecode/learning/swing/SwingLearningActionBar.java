package com.eyecode.learning.swing;

import com.eyecode.learning.model.RelatedConcept;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.util.List;
import java.util.function.Supplier;

public final class SwingLearningActionBar extends JPanel {

    private final JButton openDocsButton;
    private final JButton explainButton;
    private final JButton copyButton;
    private final JButton relatedButton;

    private LearningCardActions actions = LearningCardActions.noop();
    private Supplier<String> activeCodeSupplier = () -> null;
    private List<RelatedConcept> relatedConcepts = List.of();

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

        openDocsButton.addActionListener(e -> actions.openDocumentation());
        explainButton.addActionListener(e -> actions.explainMore());
        copyButton.addActionListener(e -> {
            String code = activeCodeSupplier != null ? activeCodeSupplier.get() : null;
            if (code != null && !code.isEmpty()) {
                actions.copyCode(code);
            }
        });
        relatedButton.addActionListener(e -> actions.showRelatedConcepts(relatedConcepts));

        add(openDocsButton);
        add(Box.createHorizontalStrut(6));
        add(explainButton);
        add(Box.createHorizontalStrut(6));
        add(copyButton);
        add(Box.createHorizontalStrut(6));
        add(relatedButton);
        add(Box.createHorizontalGlue());
    }

    public void setActions(LearningCardActions actions) {
        this.actions = actions != null ? actions : LearningCardActions.noop();
    }

    public void setActiveCodeSupplier(Supplier<String> supplier) {
        this.activeCodeSupplier = supplier != null ? supplier : () -> null;
        copyButton.setEnabled(this.activeCodeSupplier.get() != null
                && !this.activeCodeSupplier.get().isEmpty());
    }

    public void setRelatedConcepts(List<RelatedConcept> concepts) {
        this.relatedConcepts = concepts != null ? List.copyOf(concepts) : List.of();
        boolean enabled = !this.relatedConcepts.isEmpty();
        relatedButton.setEnabled(enabled);
    }

    public void setDocumentationAction(Runnable action) {
        wireAction(openDocsButton, action);
    }

    public void setExplainAction(Runnable action) {
        wireAction(explainButton, action);
        explainButton.setEnabled(action != null);
    }

    public void setCopyAction(Runnable action) {
        copyButton.addActionListener(e -> {
            if (action != null) action.run();
        });
    }

    public void setRelatedAction(Runnable action) {
        wireAction(relatedButton, action);
        relatedButton.setEnabled(action != null);
    }

    public void setRelatedEnabled(boolean enabled) {
        relatedButton.setEnabled(enabled);
    }

    public void clearActions() {
        this.actions = LearningCardActions.noop();
        this.activeCodeSupplier = () -> null;
        this.relatedConcepts = List.of();
        for (java.awt.event.ActionListener al : openDocsButton.getActionListeners()) {
            openDocsButton.removeActionListener(al);
        }
        for (java.awt.event.ActionListener al : explainButton.getActionListeners()) {
            explainButton.removeActionListener(al);
        }
        for (java.awt.event.ActionListener al : copyButton.getActionListeners()) {
            copyButton.removeActionListener(al);
        }
        for (java.awt.event.ActionListener al : relatedButton.getActionListeners()) {
            relatedButton.removeActionListener(al);
        }
        copyButton.setEnabled(false);
        relatedButton.setEnabled(false);
        explainButton.setEnabled(false);
    }

    private void wireAction(JButton button, Runnable action) {
        for (java.awt.event.ActionListener al : button.getActionListeners()) {
            button.removeActionListener(al);
        }
        if (action != null) {
            button.addActionListener(e -> action.run());
            button.setEnabled(true);
        } else {
            button.setEnabled(false);
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
