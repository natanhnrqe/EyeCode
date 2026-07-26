package com.eyecode.learning.renderer;

import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardFooterData;
import com.eyecode.learning.model.LearningCardHeaderData;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.swing.SwingLearningCard;
import com.eyecode.ui.swing.SwingPopup;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;

import java.awt.Point;

public final class SwingLearningCardRenderer implements LearningCardRenderer {

    private final SwingPopup popup;
    private final SwingLearningCard card;
    private boolean visible;

    public SwingLearningCardRenderer() {
        this.popup = new SwingPopup();
        this.card = new SwingLearningCard();
        this.popup.setContent(card);
        this.popup.setFocusableWindowState(false);
        this.visible = false;
    }

    @Override
    public void show(LearningConcept concept) {
        LearningCardDocument document = buildDocument(concept);
        card.render(document);
        popup.show();
        visible = true;
    }

    private LearningCardDocument buildDocument(LearningConcept concept) {
        LearningCardDocument doc = new LearningCardDocument();
        if (concept != null) {
            String subtitle = "";
            if (concept.getType() != null) {
                subtitle = concept.getType().name();
                if (concept.getDifficulty() != null) {
                    subtitle += " • " + concept.getDifficulty().name();
                }
            }
            doc.setHeader(new LearningCardHeaderData("java", concept.getTitle() != null ? concept.getTitle() : "", subtitle));
            doc.addHeading(concept.getTitle() != null ? concept.getTitle() : "");
            if (concept.getDescription() != null && !concept.getDescription().isBlank()) {
                doc.addParagraph(concept.getDescription());
            }
            doc.addHeading("Java");
            doc.addCodeBlock("Java", generateSampleCode(concept));
            if (concept.getRelatedConcepts() != null && !concept.getRelatedConcepts().isEmpty()) {
                doc.addHeading("Related concepts");
                for (String related : concept.getRelatedConcepts()) {
                    doc.addBullet(related);
                }
            }
        } else {
            doc.setHeader(new LearningCardHeaderData("java", "Unknown", "Unknown"));
        }
        doc.setFooter(new LearningCardFooterData("Updated:", "Today"));
        return doc;
    }

    private String generateSampleCode(LearningConcept concept) {
        String name = concept.getTitle() != null ? concept.getTitle() : "Example";
        return "public class " + name + " {\n" +
               "    // " + (concept.getDescription() != null ? concept.getDescription().replace("\n", " ") : "No description") + "\n" +
               "}\n";
    }

    @Override
    public void hide() {
        popup.hide();
        visible = false;
    }

    @Override
    public boolean isVisible() {
        return visible && popup.isVisible();
    }

    @Override
    public void update(LearningConcept concept) {
        show(concept);
    }

    @Override
    public void loadHtml(String html) {
        // Not supported; ignored in Swing renderer
    }

    @Override
    public boolean containsScreen(Point screenPoint) {
        return visible && screenPoint != null && popup.getBounds().contains(screenPoint);
    }

    @Override
    public void dispose() {
        hide();
        visible = false;
    }
}
