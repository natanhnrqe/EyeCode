package com.eyecode.learning.document;

import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.markdown.MarkdownBuilder;
import com.eyecode.learning.markdown.MarkdownDocument;
import com.eyecode.learning.markdown.MarkdownRenderer;
import com.eyecode.ui.core.UIScrollPane;
import com.eyecode.ui.core.UITextPane;
import com.eyecode.ui.core.UIViewFactory;
import com.eyecode.ui.swing.SwingUIViewFactory;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.ScrollPaneConstants;
import java.awt.BorderLayout;

public final class LearningDocumentView extends JPanel {

    private final UITextPane uiTextPane;
    private final UIScrollPane uiScrollPane;
    private final MarkdownBuilder builder;
    private final MarkdownRenderer renderer;

    public LearningDocumentView() {
        this(new SwingUIViewFactory());
    }

    public LearningDocumentView(UIViewFactory viewFactory) {
        setLayout(new BorderLayout());
        setOpaque(false);

        builder = new MarkdownBuilder();
        renderer = new MarkdownRenderer();

        uiTextPane = viewFactory.createTextPane();
        uiTextPane.getTextPane().setDocument(renderer.render(null));

        JTextPane textPane = uiTextPane.getTextPane();
        textPane.setEditable(false);
        textPane.setFocusable(false);
        textPane.setOpaque(false);
        textPane.setBackground(LearningDocumentStyle.transparent());
        textPane.setCaretColor(LearningDocumentStyle.bodyColor());
        textPane.setBorder(LearningDocumentStyle.documentBorder());
        textPane.setMargin(LearningDocumentStyle.zeroInsets());
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);

        uiScrollPane = viewFactory.createScrollPane();
        uiScrollPane.getScrollPane().setViewportView(textPane);
        uiScrollPane.getScrollPane().setBorder(null);
        uiScrollPane.getScrollPane().setOpaque(false);
        uiScrollPane.getScrollPane().getViewport().setOpaque(false);
        uiScrollPane.getScrollPane().setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        uiScrollPane.getScrollPane().setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        uiScrollPane.getScrollPane().getVerticalScrollBar().setUnitIncrement(LearningDocumentStyle.documentScrollUnit());

        add(uiScrollPane.getScrollPane(), BorderLayout.CENTER);
    }

    public void setPage(LearningPage page) {
        MarkdownDocument document = builder.build(page);
        uiTextPane.getTextPane().setDocument(renderer.render(document));
        uiTextPane.getTextPane().setCaretPosition(0);
        repaint();
    }

    public void clear() {
        uiTextPane.getTextPane().setDocument(renderer.render(null));
        uiTextPane.getTextPane().setCaretPosition(0);
        repaint();
    }

    public JTextPane getTextPane() {
        return uiTextPane.getTextPane();
    }
}
