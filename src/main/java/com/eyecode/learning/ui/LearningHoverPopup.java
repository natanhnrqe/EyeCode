package com.eyecode.learning.ui;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.ui.core.UIPopup;
import com.eyecode.ui.swing.SwingPopup;

import java.awt.Dimension;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.PointerInfo;
import java.awt.Rectangle;
import java.awt.Toolkit;

public final class LearningHoverPopup {

    private UIPopup popup;
    private LearningCard card;

    public LearningHoverPopup() {
    }

    public void setCard(LearningCard card) {
        this.card = card;
        if (popup != null) {
            popup.setContent(card);
            popup.setSize(LearningDocumentStyle.popupSize().width, LearningDocumentStyle.popupSize().height);
        }
    }

    public void show(LearningConcept concept) {
        if (card == null) {
            return;
        }

        Point screenLocation = pointerScreenLocation();
        if (screenLocation == null) {
            return;
        }

        ensurePopup();
        card.setConcept(concept);
        Dimension size = LearningDocumentStyle.popupSize();
        popup.setSize(size.width, size.height);
        Point location = adjustedPosition(screenLocation, size);
        popup.setLocation(location.x, location.y);
        popup.show();
    }

    public void update(LearningConcept concept) {
        if (card == null) {
            return;
        }

        card.setConcept(concept);
        if (popup != null && popup.isVisible()) {
            popup.revalidate();
            popup.repaint();
        }
    }

    public void hide() {
        if (popup != null) {
            popup.hide();
        }
    }

    public boolean isVisible() {
        return popup != null && popup.isVisible();
    }

    public boolean containsScreen(Point screenPoint) {
        return screenPoint != null && isVisible() && getScreenBounds().contains(screenPoint);
    }

    public Rectangle getScreenBounds() {
        return popup != null ? popup.getBounds() : new Rectangle();
    }

    private void ensurePopup() {
        if (popup != null) {
            return;
        }

        SwingPopup swingPopup = new SwingPopup();
        swingPopup.setFocusableWindowState(false);
        swingPopup.setBackground(LearningDocumentStyle.transparent());
        swingPopup.setContent(card);
        swingPopup.setSize(LearningDocumentStyle.popupSize().width, LearningDocumentStyle.popupSize().height);
        popup = swingPopup;
    }

    private static Point adjustedPosition(Point mouse, Dimension popupSize) {
        GraphicsConfiguration gc = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration();
        Rectangle screen = gc.getBounds();
        Insets insets = Toolkit.getDefaultToolkit().getScreenInsets(gc);
        int cursorOffset = LearningDocumentStyle.popupCursorOffset();

        int x = mouse.x + cursorOffset;
        int y = mouse.y + cursorOffset;

        if (x + popupSize.width > screen.x + screen.width - insets.right) {
            x = mouse.x - cursorOffset - popupSize.width;
        }
        if (y + popupSize.height > screen.y + screen.height - insets.bottom) {
            y = mouse.y - cursorOffset - popupSize.height;
        }

        x = Math.max(screen.x + insets.left, x);
        y = Math.max(screen.y + insets.top, y);
        return new Point(x, y);
    }

    private static Point pointerScreenLocation() {
        PointerInfo pointerInfo = MouseInfo.getPointerInfo();
        return pointerInfo != null ? pointerInfo.getLocation() : null;
    }
}
