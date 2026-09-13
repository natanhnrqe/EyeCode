package com.eyecode.javafx.web;

import java.util.Set;

public record LocalWebShellDevMode(boolean enabled, int vitePort) {
    static final String ENABLED_PROPERTY = "eyecode.web.dev";
    static final String PORT_PROPERTY = "eyecode.web.dev.port";
    private static final int DEFAULT_VITE_PORT = 5173;

    public static LocalWebShellDevMode fromSystemProperties() {
        boolean enabled = Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false"));
        String configuredPort = System.getProperty(PORT_PROPERTY, String.valueOf(DEFAULT_VITE_PORT)).trim();
        try {
            int port = Integer.parseInt(configuredPort);
            if (port < 1 || port > 65535) throw new NumberFormatException();
            return new LocalWebShellDevMode(enabled, port);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Invalid Vite development port: " + configuredPort, exception);
        }
    }

    String frontendUrl() {
        return "http://127.0.0.1:" + vitePort;
    }

    Set<String> allowedOrigins() {
        if (!enabled) return Set.of();
        return Set.of(frontendUrl(), "http://localhost:" + vitePort);
    }
}
