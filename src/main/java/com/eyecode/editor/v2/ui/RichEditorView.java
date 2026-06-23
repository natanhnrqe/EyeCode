package com.eyecode.editor.v2.ui;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.syntax.DocumentStyleRegistry;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.swing.SwingSyntaxRenderer;
import com.eyecode.editor.v2.ui.gutter.GutterPanel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;

public final class RichEditorView extends JPanel {

    private final EditorBuffer buffer;
    private final JTextPane textPane;
    private final StyledDocument styledDocument;
    private final JScrollPane scrollPane;
    private final GutterPanel gutterPanel;
    private final JavaSyntaxAnalyzer analyzer;
    private final SwingSyntaxRenderer renderer;
    private final DocumentStyleRegistry registry;
    private boolean refreshing;

    public RichEditorView(EditorBuffer buffer) {
        super(new BorderLayout());
        this.buffer = buffer;
        this.textPane = new JTextPane();
        this.styledDocument = textPane.getStyledDocument();
        this.scrollPane = new JScrollPane(textPane);
        this.gutterPanel = new GutterPanel(textPane);
        this.analyzer = new JavaSyntaxAnalyzer();
        this.registry = new DocumentStyleRegistry();
        this.renderer = new SwingSyntaxRenderer(styledDocument, registry);

        insertDocumentText(buffer.getDocument().getText());
        styledDocument.addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                syncToDocument();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                syncToDocument();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                // Attribute-only changes are produced by syntax rendering, not text edits.
            }
        });

        add(scrollPane, BorderLayout.CENTER);
        scrollPane.setRowHeaderView(gutterPanel);
        textPane.addCaretListener(event -> gutterPanel.refresh());
        renderSyntax();
    }

    public void refreshFromDocument() {
        refreshing = true;
        try {
            styledDocument.remove(0, styledDocument.getLength());
            insertDocumentText(buffer.getDocument().getText());
            renderSyntax();
            gutterPanel.refresh();
        } catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to refresh editor document", ex);
        } finally {
            refreshing = false;
        }
    }

    public EditorBuffer getBuffer() {
        return buffer;
    }

    public JTextPane getTextPane() {
        return textPane;
    }

    public StyledDocument getStyledDocument() {
        return styledDocument;
    }

    private void syncToDocument() {
        if (refreshing) return;
        try {
            buffer.getDocument().setText(styledDocument.getText(0, styledDocument.getLength()));
            renderSyntax();
            gutterPanel.refresh();
        } catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to sync editor document", ex);
        }
    }

    private void insertDocumentText(String text) {
        try {
            styledDocument.insertString(0, text == null ? "" : text, null);
        } catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to initialize editor document", ex);
        }
    }

    private void renderSyntax() {
        SyntaxSnapshot snapshot = analyzer.analyze(buffer.getDocument());
        renderer.render(snapshot);
    }
}
