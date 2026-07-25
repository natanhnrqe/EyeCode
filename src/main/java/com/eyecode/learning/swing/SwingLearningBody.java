package com.eyecode.learning.swing;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.ui.designsystem.ColorManager;
import com.eyecode.ui.designsystem.TypographyManager;

import com.eyecode.learning.model.LearningCardBlock;
import com.eyecode.learning.model.LearningCardDocument;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import java.awt.Component;

public final class SwingLearningBody extends JScrollPane {

    private final JPanel contentPanel;

    public SwingLearningBody() {
        contentPanel = new JPanel();
        contentPanel.setLayout(new BoxLayout(contentPanel, BoxLayout.Y_AXIS));
        contentPanel.setOpaque(false);
        contentPanel.setBorder(new EmptyBorder(16, 20, 16, 20));

        setViewportView(contentPanel);
        setBorder(null);
        setOpaque(false);
        getViewport().setOpaque(false);
        getViewport().setBackground(ColorManager.CARD_BG);
    }

    public void clear() {
        contentPanel.removeAll();
        contentPanel.revalidate();
        contentPanel.repaint();
    }

    public void addHeading(String text) {
        JLabel label = new JLabel(text);
        label.setFont(TypographyManager.monoBold(13));
        label.setForeground(ColorManager.TEXT_PRIMARY);
        label.setBorder(new EmptyBorder(10, 0, 6, 0));
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
        contentPanel.add(block);
        contentPanel.add(Box.createVerticalStrut(14));
    }

    public void addBullet(String text) {
        JLabel label = new JLabel("•  " + text);
        label.setFont(TypographyManager.monoRegular(12));
        label.setForeground(ColorManager.TEXT_PRIMARY);
        label.setBorder(new EmptyBorder(2, 0, 2, 0));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        contentPanel.add(label);
    }

    public void setDocument(LearningCardDocument document) {
        clear();
        if (document == null) return;
        for (LearningCardBlock block : document.getBlocks()) {
            if (block instanceof LearningCardBlock.HeadingBlock heading) {
                addHeading(heading.text());
            } else if (block instanceof LearningCardBlock.ParagraphBlock paragraph) {
                addParagraph(paragraph.text());
            } else if (block instanceof LearningCardBlock.CodeBlock code) {
                addCodeBlock(code.language(), code.code());
            } else if (block instanceof LearningCardBlock.BulletBlock bullet) {
                addBullet(bullet.text());
            }
        }
        contentPanel.revalidate();
        contentPanel.repaint();
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

    public JPanel getContentPanel() {
        return contentPanel;
    }
}
