package com.eyecode.browser;

import com.eyecode.browser.preview.PreviewBrowserService;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

public final class BrowserPanel extends JPanel {

    private final PreviewBrowserService service;

    public BrowserPanel(PreviewBrowserService service) {
        super(new BorderLayout());
        this.service = service;
        Component browserComponent = service.getComponent();
        if (browserComponent != null) {
            add(browserComponent, BorderLayout.CENTER);
        }
    }

    public PreviewBrowserService getService() {
        return service;
    }
}
