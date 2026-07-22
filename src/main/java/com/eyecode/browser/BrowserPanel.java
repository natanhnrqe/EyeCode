package com.eyecode.browser;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Component;

public final class BrowserPanel extends JPanel {

    private final BrowserService service;

    public BrowserPanel(BrowserService service) {
        super(new BorderLayout());
        this.service = service;
        Component browserComponent = service.getComponent();
        if (browserComponent != null) {
            add(browserComponent, BorderLayout.CENTER);
        }
    }

    public BrowserService getService() {
        return service;
    }
}
