package com.eyecode.editor.v2.ui;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.caret.CaretSynchronizationManager;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionManager;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.insert.CompletionInsertionContext;
import com.eyecode.editor.v2.completion.insert.CompletionInsertionEngine;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
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
import com.eyecode.editor.v2.ui.completion.CompletionPopup;
import com.eyecode.editor.v2.ui.gutter.GutterPanel;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.CaretListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.StyledDocument;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
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
    private final CompletionPopup completionPopup;
    private final CompletionInsertionEngine completionInsertionEngine;
    private final CompletionPrefixResolver completionPrefixResolver;
    private final DocumentListener documentListener;
    private final CaretListener caretListener;
    private final FocusAdapter focusListener;
    private SyntaxSnapshot latestSyntaxSnapshot;
    private boolean refreshing;
    private boolean suppressPopup;

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
        this.completionPopup = new CompletionPopup();
        this.completionInsertionEngine = new CompletionInsertionEngine();
        this.completionPrefixResolver = new CompletionPrefixResolver();
        this.completionPopup.setOnSelect(event -> {
            buffer.setCompletionSelection(event.getSelectedItem());
            if (event.getSelectedItem() != null) {
                suppressPopup = true;
                String currentPrefix = completionPrefixResolver.resolve(buffer.getLanguageContext());
                int caretOffset = toOffset(buffer.getDocument().getText(), buffer.getCaret());
                CompletionInsertionContext insertionContext = new CompletionInsertionContext(
                        buffer.getDocument(),
                        buffer.getCaret(),
                        event.getSelectedItem(),
                        currentPrefix
                );
                completionInsertionEngine.insert(insertionContext);
                refreshFromDocument();
                int newCaretOffset = Math.max(0, caretOffset - currentPrefix.length()
                        + event.getSelectedItem().getInsertText().length());
                buffer.setCaretPosition(toPosition(buffer.getDocument().getText(), newCaretOffset));
                suppressPopup = false;
            }
            completionPopup.hide();
        });

        insertDocumentText(buffer.getDocument().getText());
        this.documentListener = new DocumentListener() {
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
        };
        styledDocument.addDocumentListener(documentListener);

        add(scrollPane, BorderLayout.CENTER);
        scrollPane.setRowHeaderView(gutterPanel);
        this.caretListener = event -> {
            gutterPanel.refresh();
            if (completionPopup.isVisible()) {
                if (isCompletionContextValid()) {
                    completionPopup.move(textPane, textPane.getCaretPosition());
                } else {
                    completionPopup.hide();
                }
            }
        };
        textPane.addCaretListener(caretListener);
        this.focusListener = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                completionPopup.hide();
            }
        };
        textPane.addFocusListener(focusListener);
        renderSyntax();
        refreshDiagnostics();
        refreshLanguageContext();
        refreshCompletions();
    }

    public void dispose() {
        styledDocument.removeDocumentListener(documentListener);
        textPane.removeCaretListener(caretListener);
        textPane.removeFocusListener(focusListener);
        caretSync.dispose();
        buffer.clearListeners();
        completionPopup.hide();
    }

    public void refreshFromDocument() {
        if (buffer.getDocument().getText().contentEquals(getCurrentText())) {
            return;
        }
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
        String text;
        try {
            text = styledDocument.getText(0, styledDocument.getLength());
            if (text.contentEquals(buffer.getDocument().getText())) {
                return;
            }
            buffer.getDocument().setText(text);
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
        if ((text == null ? "" : text).contentEquals(getCurrentText())) {
            return;
        }
        try {
            styledDocument.insertString(0, text == null ? "" : text, null);
        } catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to initialize editor document", ex);
        }
    }

    private String getCurrentText() {
        try {
            return styledDocument.getText(0, styledDocument.getLength());
        } catch (BadLocationException ex) {
            throw new IllegalStateException("Failed to read editor document", ex);
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

        if (suppressPopup) return;

        if (!isCompletionContextValid() || buffer.getCompletionSnapshot().isEmpty()) {
            completionPopup.hide();
            return;
        }

        if (completionPopup.isVisible()) {
            completionPopup.update(buffer.getCompletionSnapshot());
            completionPopup.move(textPane, textPane.getCaretPosition());
        } else {
            completionPopup.show(textPane, buffer.getCompletionSnapshot(), textPane.getCaretPosition());
        }
    }

    private boolean isCompletionContextValid() {
        String prefix = completionPrefixResolver.resolve(buffer.getLanguageContext());
        if (prefix.length() < 1) return false;

        String text = buffer.getDocument().getText();
        int offset = toOffset(text, buffer.getCaret());
        if (offset == 0) return false;
        char charBefore = text.charAt(offset - 1);
        return Character.isJavaIdentifierPart(charBefore);
    }

    private int toOffset(String text, com.eyecode.editor.v2.EditorPosition position) {
        int line = 0;
        int column = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            if (line == position.line() && column == position.column()) {
                return offset;
            }
            char current = text.charAt(offset);
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return text.length();
    }

    private com.eyecode.editor.v2.EditorPosition toPosition(String text, int offset) {
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int line = 0;
        int column = 0;
        for (int i = 0; i < safeOffset; i++) {
            if (text.charAt(i) == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return new com.eyecode.editor.v2.EditorPosition(line, column);
    }
}
