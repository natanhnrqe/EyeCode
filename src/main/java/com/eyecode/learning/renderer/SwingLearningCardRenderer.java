package com.eyecode.learning.renderer;

import com.eyecode.learning.model.LearningCardBlock;
import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardDocumentAdapter;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.service.LearningDocumentationWindowService;
import com.eyecode.learning.ui.HoverDiagnosticLogger;
import com.eyecode.learning.swing.SwingLearningCard;
import com.eyecode.ui.swing.SwingPopup;

import java.awt.Point;
import java.awt.Toolkit;
import java.util.List;

public final class SwingLearningCardRenderer implements LearningCardRenderer {

    private final SwingPopup popup;
    private final SwingLearningCard card;
    private final LearningDocumentationWindowService docService;
    private boolean visible;

    public SwingLearningCardRenderer() {
        this.popup = new SwingPopup();
        this.card = new SwingLearningCard();
        this.docService = new LearningDocumentationWindowService();
        this.popup.setContent(card);
        this.popup.setFocusableWindowState(false);
        this.visible = false;
    }

    @Override
    public void show(LearningConcept concept) {
        HoverDiagnosticLogger.logRendererShow();
        LearningCardDocument document = buildDocument(concept);
        wireActions(concept, document);
        card.render(document);
        popup.getWindow().pack();
        java.awt.PointerInfo pointerInfo = java.awt.MouseInfo.getPointerInfo();
        java.awt.Point mouse = pointerInfo != null ? pointerInfo.getLocation() : new java.awt.Point(200, 200);
        int x = mouse.x + 12;
        int y = mouse.y + 12;
        java.awt.Dimension size = popup.getWindow().getSize();
        java.awt.Rectangle screen = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        java.awt.Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(
                java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment()
                        .getDefaultScreenDevice().getDefaultConfiguration());
        if (x + size.width > screen.x + screen.width - insets.right) {
            x = mouse.x - 12 - size.width;
        }
        if (y + size.height > screen.y + screen.height - insets.bottom) {
            y = mouse.y - 12 - size.height;
        }
        popup.setLocation(x, y);
        popup.show();
        visible = true;
    }

    private void wireActions(LearningConcept concept, LearningCardDocument document) {
        card.getActionBar().setDocumentationAction(() -> {
            if (concept != null) {
                docService.open(concept);
            }
        });

        String code = extractFirstCode(document);
        card.getActionBar().setCopyAction(code != null ? () -> copyToClipboard(code) : null);

        List<String> related = concept != null ? concept.getRelatedConcepts() : null;
        boolean hasRelated = related != null && !related.isEmpty();
        card.getActionBar().setRelatedAction(hasRelated ? () -> showRelatedToast(related) : null);
        card.getActionBar().setRelatedEnabled(hasRelated);

        card.getActionBar().setExplainAction(concept != null ? () -> docService.open(concept) : null);
    }

    private String extractFirstCode(LearningCardDocument document) {
        if (document == null) return null;
        for (LearningCardBlock block : document.getBlocks()) {
            if (block instanceof LearningCardBlock.CodeBlock cb) {
                return cb.code();
            }
        }
        return null;
    }

    private void copyToClipboard(String text) {
        java.awt.datatransfer.StringSelection selection = new java.awt.datatransfer.StringSelection(text);
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    }

    private void showRelatedToast(List<String> related) {
        System.out.println("Related concepts: " + String.join(", ", related));
    }

    private LearningCardDocument buildDocument(LearningConcept concept) {
        LearningCardDocument doc = LearningCardDocumentAdapter.fromConcept(concept);
        HoverDiagnosticLogger.logCardRender();
        return doc;
    }

    @Override
    public void hide() {
        popup.hide();
        visible = false;
    }

    @Override
    public boolean isVisible() {
        boolean result = visible && popup.isVisible();
        HoverDiagnosticLogger.logCardVisible(result);
        return result;
    }

    @Override
    public void update(LearningConcept concept) {
        show(concept);
    }

    @Override
    public void loadHtml(String html) {
        // Not supported; ignored in Swing renderer
    }

    @Override
    public boolean containsScreen(Point screenPoint) {
        return visible && screenPoint != null && popup.getBounds().contains(screenPoint);
    }

    @Override
    public void dispose() {
        hide();
        visible = false;
    }
}
