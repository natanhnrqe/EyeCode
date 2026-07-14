package com.eyecode.learning.ui;

import com.eyecode.learning.document.LearningDocumentStyle;
import com.eyecode.learning.model.LearningConcept;

import javax.swing.JWindow;
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

    private JWindow window;
    private LearningCard card;

    public LearningHoverPopup() {
    }

    public void setCard(LearningCard card) {
        log("setCard:start card=" + describe(card) + " window=" + describe(window));
        this.card = card;
        if (window != null) {
            log("setCard:rebind-content-pane window=" + describe(window));
            window.setContentPane(card);
            window.setSize(LearningDocumentStyle.popupSize());
            log("setCard:rebind-done size=" + LearningDocumentStyle.popupSize().width + "x" + LearningDocumentStyle.popupSize().height);
        }
        log("setCard:end");
    }

    public void show(LearningConcept concept) {
        log("SHOW ENTER concept=" + describe(concept) + " card=" + describe(card) + " window=" + describe(window));
        if (card == null) {
            log("show:aborted reason=card-null");
            return;
        }

        Point screenLocation = pointerScreenLocation();
        log("show:pointer screenLocation=" + describe(screenLocation));
        if (screenLocation == null) {
            log("show:aborted reason=pointer-null");
            return;
        }
        log("show:ensure-window");
        ensureWindow();
        log("show:set-concept concept=" + describe(concept));
        card.setConcept(concept);
        Dimension size = LearningDocumentStyle.popupSize();
        log("show:set-size size=" + size.width + "x" + size.height);
        window.setSize(size);
        Point location = adjustedPosition(screenLocation, size);
        log("show:set-location location=" + location.x + "," + location.y);
        window.setLocation(location);
        log("show:set-visible true window=" + describe(window));
        window.setVisible(true);
        log("show:end visible=" + isVisible() + " bounds=" + getScreenBounds());
    }

    public void update(LearningConcept concept) {
        log("update:start concept=" + describe(concept) + " card=" + describe(card) + " window=" + describe(window));
        if (card == null) {
            log("update:aborted reason=card-null");
            return;
        }

        log("update:set-concept concept=" + describe(concept));
        card.setConcept(concept);
        if (window != null && window.isVisible()) {
            log("update:refresh-window visible=true");
            window.revalidate();
            window.repaint();
            log("update:refresh-done");
        } else {
            log("update:skip-refresh visible=false window=" + describe(window));
        }
        log("update:end");
    }

    public void hide() {
        log("hide:start window=" + describe(window) + " visible=" + isVisible());
        if (window != null) {
            log("hide:set-visible false");
            window.setVisible(false);
        } else {
            log("hide:skipped reason=window-null");
        }
        log("hide:end visible=" + isVisible());
    }

    public boolean isVisible() {
        return window != null && window.isVisible();
    }

    public boolean containsScreen(Point screenPoint) {
        return screenPoint != null && isVisible() && getScreenBounds().contains(screenPoint);
    }

    public Rectangle getScreenBounds() {
        return window != null ? window.getBounds() : new Rectangle();
    }

    private void ensureWindow() {
        log("ensureWindow:enter window=" + describe(window));
        if (window != null) {
            log("ensureWindow:reuse window=" + describe(window));
            return;
        }

        log("ensureWindow:create");
        window = new JWindow();
        window.setFocusableWindowState(false);
        window.setBackground(LearningDocumentStyle.transparent());
        window.setContentPane(card);
        window.setSize(LearningDocumentStyle.popupSize());
        log("ensureWindow:created window=" + describe(window) + " size=" + LearningDocumentStyle.popupSize().width + "x" + LearningDocumentStyle.popupSize().height);
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

    private static void log(String message) {
        System.out.println("[LearningHoverPopup] " + message);
    }

    private static String describe(Object value) {
        if (value == null) {
            return "null";
        }
        return value.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(value));
    }
}
