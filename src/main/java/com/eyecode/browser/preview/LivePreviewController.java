package com.eyecode.browser.preview;

import com.eyecode.browser.BrowserService;
import com.eyecode.editor.v2.integration.EditorHostPanel;
import com.eyecode.editor.v2.ui.RichEditorView;

import javax.swing.Timer;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.io.File;
import java.util.Locale;

public final class LivePreviewController {

    private static final int DEBOUNCE_MS = 300;

    private final BrowserService browserService;
    private final EditorHostPanel editorHostPanel;
    private final Timer debounceTimer;
    private final ChangeListener tabChangeListener;

    private DocumentListener currentDocListener;
    private Document currentDocument;

    public LivePreviewController(BrowserService browserService, EditorHostPanel editorHostPanel) {
        this.browserService = browserService;
        this.editorHostPanel = editorHostPanel;

        this.debounceTimer = new Timer(DEBOUNCE_MS, e -> refresh());
        this.debounceTimer.setRepeats(false);

        this.tabChangeListener = e -> onActiveTabChanged();
        this.editorHostPanel.addChangeListener(tabChangeListener);

        onActiveTabChanged();
    }

    private void onActiveTabChanged() {
        removeCurrentListener();

        if (!isHtmlFile()) {
            return;
        }

        RichEditorView view = editorHostPanel.getActiveView();
        if (view == null) {
            return;
        }

        currentDocument = view.getStyledDocument();
        currentDocListener = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { schedulePreview(); }
            @Override public void removeUpdate(DocumentEvent e) { schedulePreview(); }
            @Override public void changedUpdate(DocumentEvent e) { schedulePreview(); }
        };
        currentDocument.addDocumentListener(currentDocListener);

        schedulePreview();
    }

    private void removeCurrentListener() {
        if (currentDocListener != null && currentDocument != null) {
            currentDocument.removeDocumentListener(currentDocListener);
        }
        currentDocListener = null;
        currentDocument = null;
    }

    private void schedulePreview() {
        debounceTimer.restart();
    }

    private void refresh() {
        if (!isHtmlFile()) {
            return;
        }

        RichEditorView view = editorHostPanel.getActiveView();
        if (view == null) {
            return;
        }

        Document doc = view.getStyledDocument();
        String text;
        try {
            text = doc.getText(0, doc.getLength());
        } catch (Exception e) {
            return;
        }

        if (text == null || text.isBlank()) {
            browserService.showWelcomePage();
        } else {
            browserService.previewHtml(text);
        }
    }

    private boolean isHtmlFile() {
        File file = editorHostPanel.getActiveFile();
        if (file == null) {
            return false;
        }
        String name = file.getName().toLowerCase(Locale.ROOT);
        return name.endsWith(".html") || name.endsWith(".htm");
    }
}
