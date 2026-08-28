package com.eyecode.javafx.web;

import java.net.URL;

public final class WebShellAssetResolver {
    private static final String DEV_URL_PROPERTY = "eyecode.webshell.devUrl";
    private static final String ENTRY_RESOURCE = "/webshell/index.html";

    public String entryUrl() {
        String developmentUrl = System.getProperty(DEV_URL_PROPERTY);
        if (developmentUrl != null && !developmentUrl.isBlank()) {
            return developmentUrl.trim();
        }
        URL resource = WebShellAssetResolver.class.getResource(ENTRY_RESOURCE);
        if (resource == null) {
            throw new IllegalStateException("Web Shell entry asset is missing: " + ENTRY_RESOURCE);
        }
        return resource.toExternalForm();
    }

    public boolean development() {
        String developmentUrl = System.getProperty(DEV_URL_PROPERTY);
        return developmentUrl != null && !developmentUrl.isBlank();
    }
}
