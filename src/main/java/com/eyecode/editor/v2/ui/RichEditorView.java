package com.eyecode.editor.v2.ui;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.caret.CaretSynchronizationManager;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionManager;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticSymbolRegistry;
import com.eyecode.editor.v2.diagnostics.DiagnosticManager;
import com.eyecode.editor.v2.diagnostics.EmptyDiagnosticEngine;
import com.eyecode.editor.v2.language.DefaultLanguageService;
import com.eyecode.editor.v2.language.LanguageManager;
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
import java.util.List;

public final class RichEditorView extends JPanel {

    private final EditorBuffer buffer;
    private final JTextPane textPane;
    private final StyledDocument styledDocument;
    private final JScrollPane scrollPane;
    private final GutterPanel gutterPanel;
    private final JavaSyntaxAnalyzer analyzer;
    private final SwingSyntaxRenderer renderer;
    private final DocumentStyleRegistry registry;
    private final CaretSynchronizationManager caretSync;
    private final DiagnosticManager diagnosticManager;
    private final LanguageManager languageManager;
    private final CompletionManager completionManager;
    private SyntaxSnapshot latestSyntaxSnapshot;
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
        this.caretSync = new CaretSynchronizationManager(textPane, buffer);
        this.diagnosticManager = new DiagnosticManager(new EmptyDiagnosticEngine());
        this.languageManager = new LanguageManager(new DefaultLanguageService());
        this.completionManager = new CompletionManager(new CompletionEngine(
                List.of(
                        new JavaKeywordCompletionProvider(),
                        new SemanticCompletionProvider(new SemanticSymbolRegistry())
                )
        ));

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
        refreshDiagnostics();
        refreshLanguageContext();
        refreshCompletions();
    }

    public void refreshFromDocument() {
        refreshing = true;
        try {
            styledDocument.remove(0, styledDocument.getLength());
            insertDocumentText(buffer.getDocument().getText());
            renderSyntax();
            refreshDiagnostics();
            refreshLanguageContext();
            refreshCompletions();
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
            refreshDiagnostics();
            refreshLanguageContext();
            refreshCompletions();
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
        latestSyntaxSnapshot = analyzer.analyze(buffer.getDocument());
        renderer.render(latestSyntaxSnapshot);
    }

    private void refreshDiagnostics() {
        diagnosticManager.refresh(buffer.getDocument());
        buffer.setDiagnostics(diagnosticManager.getSnapshot());
    }

    private void refreshLanguageContext() {
        languageManager.refresh(buffer, latestSyntaxSnapshot);
        buffer.setLanguageContext(languageManager.getContext());
    }

    private void refreshCompletions() {
        completionManager.refresh(buffer.getLanguageContext());
        buffer.setCompletionSnapshot(completionManager.getSnapshot());
    }
}
