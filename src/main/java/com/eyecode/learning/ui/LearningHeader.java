package com.eyecode.learning.ui;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.ui.core.UILabel;
import com.eyecode.ui.core.UIViewFactory;
import com.eyecode.ui.designsystem.IconManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Component;

public final class LearningHeader extends JPanel {

    private final UILabel titleLabel;
    private final UILabel subtitleLabel;

    public LearningHeader(UIViewFactory viewFactory) {
        super(new BorderLayout());
        setOpaque(false);

        titleLabel = viewFactory.createLabel();
        titleLabel.getLabel().setFont(LearningDocumentStyle.titleFont());
        titleLabel.getLabel().setForeground(LearningDocumentStyle.titleColor());
        titleLabel.getLabel().setIcon(IconManager.javaFile());
        titleLabel.getLabel().setIconTextGap(LearningDocumentStyle.titleIconTextGap());
        titleLabel.getLabel().setBorder(LearningDocumentStyle.emptyBorder());
        titleLabel.getLabel().setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitleLabel = viewFactory.createLabel();
        subtitleLabel.getLabel().setFont(LearningDocumentStyle.subtitleFont());
        subtitleLabel.getLabel().setForeground(LearningDocumentStyle.subtitleColor());
        subtitleLabel.getLabel().setBorder(LearningDocumentStyle.emptyBorder());
        subtitleLabel.getLabel().setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textPanel = (JPanel) viewFactory.createContainer().getComponent();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(titleLabel.getLabel());
        textPanel.add(Box.createVerticalStrut(LearningDocumentStyle.headerTextGap()));
        textPanel.add(subtitleLabel.getLabel());

        JPanel content = (JPanel) viewFactory.createContainer().getComponent();
        content.setLayout(new BorderLayout());
        content.setOpaque(false);
        content.setBorder(LearningDocumentStyle.headerBorder());
        content.add(textPanel, BorderLayout.CENTER);

        JSeparator separator = new JSeparator();
        separator.setForeground(LearningDocumentStyle.dividerColor());

        add(content, BorderLayout.CENTER);
        add(separator, BorderLayout.SOUTH);
    }

    public void setConcept(LearningConcept concept) {
        if (concept == null) {
            titleLabel.getLabel().setText("");
            subtitleLabel.getLabel().setText("");
            return;
        }

        titleLabel.getLabel().setText(nullToEmpty(concept.getTitle()));
        subtitleLabel.getLabel().setText(typeDisplayName(concept.getType()) + " \u2022 " + difficultyDisplayName(concept.getDifficulty()));
    }

    private static String typeDisplayName(ConceptType type) {
        if (type == null) return "";
        String name = type.name();
        return name.charAt(0) + name.substring(1).toLowerCase();
    }

    private static String difficultyDisplayName(DifficultyLevel difficulty) {
        if (difficulty == null) return "";
        return switch (difficulty) {
            case BEGINNER -> "Iniciante";
            case INTERMEDIATE -> "Intermedi\u00e1rio";
            case ADVANCED -> "Avan\u00e7ado";
        };
    }

    private static String nullToEmpty(String text) {
        return text != null ? text : "";
    }
}
