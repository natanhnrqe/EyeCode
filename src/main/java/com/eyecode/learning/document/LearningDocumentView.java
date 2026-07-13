package com.eyecode.learning.document;

import com.eyecode.learning.content.LearningPage;
import com.eyecode.ui.designsystem.ColorManager;

import javax.swing.*;
import javax.swing.text.StyledDocument;
import java.awt.*;

public final class LearningDocumentView extends JPanel {

    private final JTextPane textPane;
    private final JScrollPane scrollPane;
    private final LearningDocumentRenderer renderer;

    public LearningDocumentView() {
        setLayout(new BorderLayout());
        setOpaque(false);

        renderer = new LearningDocumentRenderer();

        textPane = new JTextPane(renderer.getDocument()) {
            @Override
            public boolean getScrollableTracksViewportWidth() {
                return true;
            }
        };
        textPane.setEditable(false);
        textPane.setOpaque(false);
        textPane.setBackground(new Color(0, 0, 0, 0));
        textPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        textPane.setMargin(new Insets(0, 0, 0, 0));
        textPane.setCaretColor(ColorManager.TEXT_SECONDARY);

        scrollPane = new JScrollPane(textPane);
        scrollPane.setBorder(null);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        add(scrollPane, BorderLayout.CENTER);
    }

    public void setPage(LearningPage page) {
        StyledDocument doc = renderer.render(page);
        textPane.setDocument(doc);
        textPane.setCaretPosition(0);
    }

    public void clear() {
        renderer.clear();
        textPane.setDocument(renderer.getDocument());
    }

    public JTextPane getTextPane() {
        return textPane;
    }
}
