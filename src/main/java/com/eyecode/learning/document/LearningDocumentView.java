package com.eyecode.learning.document;

import com.eyecode.learning.content.LearningPage;

import javax.swing.*;
import java.awt.*;

public final class LearningDocumentView extends JPanel {

    private final JTextPane textPane;
    private final JScrollPane scrollPane;
    private final LearningDocumentRenderer renderer;

    public LearningDocumentView() {
        setLayout(new BorderLayout());
        setOpaque(false);

        renderer = new LearningDocumentRenderer();

        textPane = new JTextPane();
        textPane.setDocument(renderer.getDocument());

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
        textPane.setDocument(renderer.render(page));
        textPane.setCaretPosition(0);
        repaint();
    }

    public void clear() {
        renderer.clear();
        textPane.setDocument(renderer.getDocument());
        textPane.setCaretPosition(0);
        repaint();
    }

    public JTextPane getTextPane() {
        return textPane;
    }
}
