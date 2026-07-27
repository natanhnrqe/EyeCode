package com.eyecode.learning.swing;

import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardFooterData;
import com.eyecode.learning.model.LearningCardHeaderData;
import com.eyecode.learning.model.RelatedConcept;

import com.eyecode.ui.designsystem.IconManager;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.List;

public final class SwingLearningCard extends JPanel {

    private final SwingLearningHeader header;
    private final SwingLearningActionBar actionBar;
    private final SwingLearningBody body;
    private final SwingLearningFooter footer;

    public SwingLearningCard() {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(SwingLearningCardStyle.CARD_BACKGROUND);
        setBorder(SwingLearningCardStyle.cardBorder());

        header = new SwingLearningHeader();
        actionBar = new SwingLearningActionBar();
        body = new SwingLearningBody();
        footer = new SwingLearningFooter();
        footer.setVisible(false);

        JPanel centerArea = new JPanel(new BorderLayout());
        centerArea.setOpaque(false);
        centerArea.add(actionBar, BorderLayout.NORTH);
        centerArea.add(body, BorderLayout.CENTER);

        add(header, BorderLayout.NORTH);
        add(centerArea, BorderLayout.CENTER);
        add(footer, BorderLayout.SOUTH);

        setMaximumSize(new Dimension(
                SwingLearningCardStyle.CARD_MAX_WIDTH,
                SwingLearningCardStyle.CARD_MAX_HEIGHT));
    }

    public SwingLearningHeader getHeader() {
        return header;
    }

    public SwingLearningBody getBody() {
        return body;
    }

    public SwingLearningFooter getFooter() {
        return footer;
    }

    public SwingLearningActionBar getActionBar() {
        return actionBar;
    }

    public void render(LearningCardDocument document) {
        if (document == null) {
            clear();
            return;
        }
        if (document.getHeader() != null) {
            LearningCardHeaderData headerData = document.getHeader();
            header.setTitle(headerData.title() != null ? headerData.title() : "");
            header.setSubtitle(headerData.subtitle() != null ? headerData.subtitle() : "");
            header.setIcon(headerData.iconKey() != null && !headerData.iconKey().isBlank()
                    ? IconManager.javaFile() : null);
        } else {
            header.setTitle("");
            header.setSubtitle("");
            header.setIcon(null);
        }

        body.setDocument(document);

        if (document.getFooter() != null) {
            LearningCardFooterData footerData = document.getFooter();
            footer.setFooterText(
                    footerData.updatedLabel() != null ? footerData.updatedLabel() : "",
                    footerData.updatedValue() != null ? footerData.updatedValue() : "");
            footer.setVisible(true);
        } else {
            footer.setFooterText("", "");
            footer.setVisible(false);
        }
    }

    public void bindActions(LearningCardActions actions, List<RelatedConcept> relatedConcepts) {
        actionBar.setActions(actions);
        actionBar.setRelatedConcepts(relatedConcepts);
        body.setRelatedConcepts(relatedConcepts);
        body.setOnRelatedConceptSelect(rc -> actions.showRelatedConcepts(List.of(rc)));
        String joined = body.getAllCodeJoined();
        if (!joined.isEmpty()) {
            actionBar.setActiveCodeSupplier(body::getAllCodeJoined);
        } else {
            actionBar.setActiveCodeSupplier(() -> null);
        }
    }

    public void clear() {
        header.setTitle("");
        header.setSubtitle("");
        header.setIcon(null);
        body.clear();
        footer.setFooterText("", "");
        footer.setVisible(false);
        actionBar.clearActions();
    }
}
