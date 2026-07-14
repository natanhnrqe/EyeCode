package com.eyecode.learning.ui;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.ui.components.LearningSubtitle;
import com.eyecode.learning.ui.components.LearningTitle;
import com.eyecode.ui.designsystem.IconManager;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import java.awt.BorderLayout;
import java.awt.Component;

public final class LearningHeader extends JPanel {

    private final LearningTitle title;
    private final LearningSubtitle subtitle;

    public LearningHeader() {
        super(new BorderLayout());
        setOpaque(false);

        title = new LearningTitle("", IconManager.javaFile());
        title.setAlignmentX(Component.LEFT_ALIGNMENT);

        subtitle = new LearningSubtitle("");
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);
        textPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        textPanel.add(title);
        textPanel.add(Box.createVerticalStrut(LearningDocumentStyle.headerTextGap()));
        textPanel.add(subtitle);

        JPanel content = new JPanel(new BorderLayout());
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
            title.setText("");
            subtitle.setText("");
            return;
        }

        title.setText(nullToEmpty(concept.getTitle()));
        subtitle.setText(typeDisplayName(concept.getType()) + " \u2022 " + difficultyDisplayName(concept.getDifficulty()));
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
