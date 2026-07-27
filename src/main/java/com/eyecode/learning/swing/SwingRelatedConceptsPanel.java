package com.eyecode.learning.swing;

import com.eyecode.learning.model.RelatedConcept;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class SwingRelatedConceptsPanel extends JPanel {

    private final List<RelatedConcept> concepts = new ArrayList<>();
    private Consumer<RelatedConcept> onSelect;

    public SwingRelatedConceptsPanel() {
        super();
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(BorderFactory.createEmptyBorder(4, 0, 4, 0));
        setAlignmentX(Component.LEFT_ALIGNMENT);
    }

    public void setConcepts(List<RelatedConcept> concepts) {
        this.concepts.clear();
        removeAll();
        if (concepts == null || concepts.isEmpty()) {
            revalidate();
            repaint();
            return;
        }
        this.concepts.addAll(concepts);
        for (RelatedConcept rc : concepts) {
            if (rc == null || rc.id() == null || rc.id().isBlank()) {
                continue;
            }
            JButton btn = new JButton(rc.title() != null ? rc.title() : rc.id());
            btn.setFont(SwingLearningCardStyle.actionBarButtonFont());
            btn.setForeground(SwingLearningCardStyle.ACTION_BAR_FG_ENABLED);
            btn.setBackground(SwingLearningCardStyle.ACTION_BAR_BG_NORMAL);
            btn.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
            btn.setOpaque(false);
            btn.setContentAreaFilled(false);
            btn.setBorderPainted(true);
            btn.setBorder(BorderFactory.createLineBorder(SwingLearningCardStyle.ACTION_BAR_BORDER, 1));
            btn.setAlignmentX(Component.LEFT_ALIGNMENT);
            btn.addActionListener(e -> {
                if (onSelect != null && rc != null) {
                    onSelect.accept(rc);
                }
            });
            add(btn);
            add(javax.swing.Box.createVerticalStrut(4));
        }
        revalidate();
        repaint();
    }

    public void setOnSelect(Consumer<RelatedConcept> onSelect) {
        this.onSelect = onSelect;
    }

    public List<RelatedConcept> concepts() {
        return List.copyOf(concepts);
    }
}
