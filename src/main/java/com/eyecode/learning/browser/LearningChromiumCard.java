package com.eyecode.learning.browser;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;

public final class LearningChromiumCard extends JPanel {

    // TEMP EXPERIMENT FLAG: set false to bypass CEF (used by Experiment B)
    public static boolean USE_CEF = true;

    private LearningBrowserService browserService;
    private JEditorPane fallbackPane;

    public LearningChromiumCard() {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(new java.awt.Color(0x1e, 0x1e, 0x1e));
        setBorder(javax.swing.BorderFactory.createEmptyBorder());

        if (!USE_CEF) {
            fallbackPane = new JEditorPane("text/html", "<html><body style='color:#ccc;background:#1e1e1e;padding:16px'><p>CEF disabled</p></body></html>");
            fallbackPane.setEditable(false);
            fallbackPane.setBackground(new java.awt.Color(0x1e, 0x1e, 0x1e));
            add(new JScrollPane(fallbackPane), BorderLayout.CENTER);
        }
    }

    private void ensureBrowserCreated() {
        if (browserService != null) return;
        browserService = new LearningBrowserService();
        var browserComponent = browserService.getComponent();
        if (browserComponent != null) {
            browserComponent.setFocusable(false);
            add(browserComponent, BorderLayout.CENTER);
            revalidate();
        }
    }

    public void loadHtml(String html) {
        if (!USE_CEF && fallbackPane != null) {
            fallbackPane.setText(html);
            return;
        }
        ensureBrowserCreated();
        browserService.loadHtml(html);
    }

    public void loadUrl(String url) {
        if (!USE_CEF) return;
        ensureBrowserCreated();
        browserService.loadUrl(url);
    }

    public void reload() {
        if (!USE_CEF && fallbackPane != null) {
            fallbackPane.setText("<html><body style='color:#ccc;background:#1e1e1e;padding:16px'><p>Reloaded</p></body></html>");
            return;
        }
        ensureBrowserCreated();
        browserService.reload();
    }

    public void scrollToAnchor(String anchor) {
        if (!USE_CEF) return;
        ensureBrowserCreated();
        browserService.scrollToAnchor(anchor);
    }

    public void dispose() {
        if (browserService != null) {
            browserService.dispose();
            var browserComponent = browserService.getComponent();
            if (browserComponent != null) {
                remove(browserComponent);
            }
            browserService = null;
            revalidate();
            repaint();
        }
    }
}
