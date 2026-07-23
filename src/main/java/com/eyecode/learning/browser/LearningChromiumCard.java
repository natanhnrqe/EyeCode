package com.eyecode.learning.browser;

import javax.swing.JPanel;
import java.awt.BorderLayout;

public final class LearningChromiumCard extends JPanel {

    private final LearningBrowserService browserService;

    public LearningChromiumCard() {
        super(new BorderLayout());
        setOpaque(true);
        setBackground(new java.awt.Color(0x1e, 0x1e, 0x1e));
        setBorder(javax.swing.BorderFactory.createEmptyBorder());

        browserService = new LearningBrowserService();
        add(browserService.getComponent(), BorderLayout.CENTER);
    }

    public void loadHtml(String html) {
        browserService.loadHtml(html);
    }

    public void loadUrl(String url) {
        browserService.loadUrl(url);
    }

    public void reload() {
        browserService.reload();
    }

    public void scrollToAnchor(String anchor) {
        browserService.scrollToAnchor(anchor);
    }

    public void dispose() {
        browserService.dispose();
    }
}
