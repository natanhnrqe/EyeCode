package com.eyecode.learning.swing;

import com.eyecode.learning.model.LearningCardBlock;
import com.eyecode.learning.model.LearningCardDocument;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class SwingLearningBody extends JScrollPane {

    private final JPanel contentPanel;
    private final List<SwingCodeBlock> codeBlocks = new ArrayList<>();
    private final SwingRelatedConceptsPanel relatedPanel;

    public SwingLearningBody() {
        this.relatedPanel = new SwingRelatedConceptsPanel();
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(SwingLearningCardStyle.bodyContentBorder());

        setViewportView(contentPanel);
        setBorder(null);
        setOpaque(false);
        getViewport().setOpaque(false);
        getViewport().setBackground(SwingLearningCardStyle.BODY_VIEWPORT_BG);
        setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        getVerticalScrollBar().setUnitIncrement(16);
        getVerticalScrollBar().setPreferredSize(
                new java.awt.Dimension(SwingLearningCardStyle.SCROLLBAR_WIDTH, 0));
    }

    public void clear() {
        contentPanel.removeAll();
        codeBlocks.clear();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void addHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(SwingLearningCardStyle.bodyHeadingFont());
        label.setForeground(SwingLearningCardStyle.BODY_HEADING_COLOR);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(
                SwingLearningCardStyle.BODY_HEADING_TOP_GAP, 0,
                SwingLearningCardStyle.BODY_HEADING_BOTTOM_GAP, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(label);
    }

    public void addParagraph(String text) {
        SwingLearningParagraph paragraph = new SwingLearningParagraph(text);
        paragraph.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(paragraph);
    }

    public void addCodeBlock(String language, String code) {
        SwingCodeBlock block = new SwingCodeBlock(language, code);
        block.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(block);
        contentPanel.add(Box.createVerticalStrut(SwingLearningCardStyle.BODY_CODE_BOTTOM_GAP));
        codeBlocks.add(block);
    }

    public void addBullet(String text) {
        SwingLearningBullet bullet = new SwingLearningBullet(text);
        bullet.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(bullet);
    }

    public void setDocument(LearningCardDocument document) {
        clear();
        if (document == null) return;
        for (LearningCardBlock block : document.getBlocks()) {
            if (block instanceof LearningCardBlock.HeadingBlock heading) {
                if (heading.text() != null && !heading.text().isBlank()) {
                    addHeading(heading.text());
                }
            } else if (block instanceof LearningCardBlock.ParagraphBlock paragraph) {
                if (paragraph.text() != null && !paragraph.text().isBlank()) {
                    addParagraph(paragraph.text());
                }
            } else if (block instanceof LearningCardBlock.CodeBlock code) {
                if (code.code() != null && !code.code().isBlank()) {
                    addCodeBlock(code.language(), code.code());
                }
            } else if (block instanceof LearningCardBlock.BulletBlock bullet) {
                if (bullet.text() != null && !bullet.text().isBlank()) {
                    addBullet(bullet.text());
                }
            }
        }
        java.util.List<com.eyecode.learning.model.RelatedConcept> docRelated = document.getRelatedConcepts();
        if (docRelated != null && !docRelated.isEmpty()) {
            if (contentPanel.getComponentZOrder(relatedPanel) == -1) {
                contentPanel.add(relatedPanel);
            }
            relatedPanel.setConcepts(docRelated);
        } else {
            if (contentPanel.getComponentZOrder(relatedPanel) != -1) {
                contentPanel.remove(relatedPanel);
            }
            relatedPanel.setConcepts(List.of());
        }
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public List<SwingCodeBlock> getCodeBlocks() {
        return Collections.unmodifiableList(codeBlocks);
    }

    public SwingCodeBlock getFirstCodeBlock() {
        return codeBlocks.isEmpty() ? null : codeBlocks.get(0);
    }

    public String getAllCodeJoined() {
        if (codeBlocks.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < codeBlocks.size(); i++) {
            if (i > 0) {
                sb.append("\n\n");
            }
            sb.append(codeBlocks.get(i).code());
        }
        return sb.toString();
    }

    public boolean hasCode() {
        return !codeBlocks.isEmpty();
    }

    public void buildFixture() {
        LearningCardDocument document = new LearningCardDocument();
        document.addHeading("Class");
        document.addParagraph("A class defines the structure and behavior of an object.");
        document.addHeading("Inheritance");
        document.addParagraph("Inheritance allows a class to reuse and extend behavior from another class.");
        document.addHeading("Java");
        document.addCodeBlock("Java",
                "public class Animal {\n" +
                "    public void speak() {\n" +
                "        System.out.println(\"Hello\");\n" +
                "    }\n" +
                "}\n"
        );
        document.addHeading("Related concepts");
        document.addBullet("Object");
        document.addBullet("Inheritance");
        document.addBullet("Polymorphism");
        setDocument(document);
    }

    public void setRelatedConcepts(java.util.List<com.eyecode.learning.model.RelatedConcept> concepts) {
        relatedPanel.setConcepts(concepts);
    }

    public void setOnRelatedConceptSelect(java.util.function.Consumer<com.eyecode.learning.model.RelatedConcept> onSelect) {
        relatedPanel.setOnSelect(onSelect);
    }

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
