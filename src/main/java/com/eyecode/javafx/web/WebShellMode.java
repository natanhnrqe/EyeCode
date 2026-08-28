package com.eyecode.javafx.web;

public enum WebShellMode {
    LEGACY_JAVAFX,
    WEB_SHELL;

    public static WebShellMode configured() {
        String value = System.getProperty("eyecode.ui", "LEGACY_JAVAFX");
        try {
            return valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return LEGACY_JAVAFX;
        }
    }
}
