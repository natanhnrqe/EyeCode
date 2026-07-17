package com.eyecode.learning.document;

import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.markdown.MarkdownBuilder;
import com.eyecode.learning.markdown.MarkdownDocument;
import com.eyecode.learning.markdown.MarkdownRenderer;

import javax.swing.*;
import java.awt.*;

public final class LearningDocumentView extends JPanel {

    private final JTextPane textPane;
    private final JScrollPane scrollPane;
    private final MarkdownBuilder builder;
    private final MarkdownRenderer renderer;

    public LearningDocumentView() {
        setLayout(new BorderLayout());
        setOpaque(false);

        builder = new MarkdownBuilder();
        renderer = new MarkdownRenderer();

        textPane = new JTextPane();
        textPane.setDocument(renderer.render(null));

        textPane.setEditable(false);
        textPane.setFocusable(false);
        textPane.setOpaque(false);
        textPane.setBackground(LearningDocumentStyle.transparent());
        textPane.setCaretColor(LearningDocumentStyle.bodyColor());
        textPane.setBorder(LearningDocumentStyle.documentBorder());
        textPane.setMargin(LearningDocumentStyle.zeroInsets());
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(LearningDocumentStyle.documentScrollUnit());

        add(scrollPane, BorderLayout.CENTER);
    }

    public void setPage(LearningPage page) {
        MarkdownDocument document = builder.build(page);
        textPane.setDocument(renderer.render(document));
        textPane.setCaretPosition(0);
        repaint();
    }

    public void clear() {
        textPane.setDocument(renderer.render(null));
        textPane.setCaretPosition(0);
        repaint();
    }

    public JTextPane getTextPane() {
        return textPane;
    }
}
