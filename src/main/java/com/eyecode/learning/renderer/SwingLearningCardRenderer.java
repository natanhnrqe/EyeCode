package com.eyecode.learning.renderer;

import com.eyecode.learning.catalog.CatalogRelatedConceptResolver;
import com.eyecode.learning.catalog.LearningCatalog;
import com.eyecode.learning.model.LearningCardDocument;
import com.eyecode.learning.model.LearningCardDocumentAdapter;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.RelatedConcept;
import com.eyecode.learning.model.RelatedConceptNavigator;
import com.eyecode.learning.model.RelatedConceptResolver;
import com.eyecode.learning.service.DocumentationLearningCardActions;
import com.eyecode.learning.service.DocumentationOpener;
import com.eyecode.learning.service.ExplainMoreHandler;
import com.eyecode.learning.service.LearningDocumentationWindowService;
import com.eyecode.learning.ui.HoverDiagnosticLogger;
import com.eyecode.learning.swing.LearningCardActions;
import com.eyecode.learning.swing.SwingLearningCard;
import com.eyecode.ui.swing.SwingPopup;

import java.awt.Point;
import java.awt.Toolkit;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class SwingLearningCardRenderer implements LearningCardRenderer {

    private final SwingPopup popup;
    private final SwingLearningCard card;
    private final DocumentationOpener documentationOpener;
    private final RelatedConceptResolver relatedResolver;
    private final ExplainMoreHandler explainMoreHandler;
    private boolean visible;
    private LearningCardActions currentActions;

    public SwingLearningCardRenderer() {
        this(new LearningDocumentationWindowService());
    }

    public SwingLearningCardRenderer(LearningDocumentationWindowService docService) {
        this(docService::open,
                RelatedConceptResolver.empty(),
                ExplainMoreHandler.delegatingTo(docService::open));
    }

    public SwingLearningCardRenderer(DocumentationOpener documentationOpener,
                                      RelatedConceptResolver relatedResolver,
                                      ExplainMoreHandler explainMoreHandler) {
        this.popup = new SwingPopup();
        this.card = new SwingLearningCard();
        this.documentationOpener = Objects.requireNonNull(documentationOpener,
                "documentationOpener must not be null");
        this.relatedResolver = Objects.requireNonNull(relatedResolver, "relatedResolver must not be null");
        this.explainMoreHandler = Objects.requireNonNull(explainMoreHandler, "explainMoreHandler must not be null");
        this.popup.setContent(card);
        this.popup.setFocusableWindowState(false);
        this.visible = false;
    }

    public static SwingLearningCardRenderer withCatalog(LearningCatalog catalog) {
        Objects.requireNonNull(catalog, "catalog must not be null");
        LearningDocumentationWindowService docService = new LearningDocumentationWindowService();
        RelatedConceptResolver resolver = new CatalogRelatedConceptResolver(catalog);
        ExplainMoreHandler explain = ExplainMoreHandler.delegatingTo(docService::open);
        return new SwingLearningCardRenderer(docService::open, resolver, explain);
    }

    public static SwingLearningCardRenderer withOpener(DocumentationOpener opener,
                                                       RelatedConceptResolver resolver,
                                                       ExplainMoreHandler explainMoreHandler) {
        return new SwingLearningCardRenderer(opener, resolver, explainMoreHandler);
    }

    @Override
    public void show(LearningConcept concept) {
        HoverDiagnosticLogger.logRendererShow();
        renderConcept(concept);
        popup.getWindow().pack();
        positionPopup();
        popup.show();
        visible = true;
    }

    @Override
    public void update(LearningConcept concept) {
        if (!visible) {
            show(concept);
            return;
        }
        renderConcept(concept);
        // Manter posição atual quando já visível; não reposicionar ao navegar via Related Concepts
        popup.getWindow().pack();
        // NÃO chamar positionPopup() para evitar reposicionar o card quando o usuário está interagindo
        // A posição só é atualizada quando o card é mostrado pela primeira vez (show())
    }

    private void renderConcept(LearningConcept concept) {
        LearningCardDocument document = LearningCardDocumentAdapter.fromConcept(concept);
        HoverDiagnosticLogger.logCardRender();
        List<RelatedConcept> related = document.getRelatedConcepts();
        Consumer<LearningConcept> onNavigate = this::onRelatedConceptNavigated;
        RelatedConceptNavigator navigator = new RelatedConceptNavigator(relatedResolver, onNavigate);
        this.currentActions = new DocumentationLearningCardActions(
                documentationOpener, explainMoreHandler, navigator, concept, related);
        card.render(document);
        card.bindActions(currentActions, related);
    }

    private void onRelatedConceptNavigated(LearningConcept concept) {
        update(concept);
    }

    private void positionPopup() {
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
        card.clear();
        this.currentActions = null;
        this.visible = false;
    }

    LearningCardActions currentActionsForTest() {
        return currentActions;
    }
}

