package com.eyecode.ui.web;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalWebShellDevModeTest {
    @AfterEach
    void clearProperties() {
        System.clearProperty(LocalWebShellDevMode.ENABLED_PROPERTY);
        System.clearProperty(LocalWebShellDevMode.PORT_PROPERTY);
    }

    @Test
    void normalModeIsTheDeterministicDefault() {
        LocalWebShellDevMode mode = LocalWebShellDevMode.fromSystemProperties();

        assertFalse(mode.enabled());
        assertEquals("http://127.0.0.1:5173", mode.frontendUrl());
        assertTrue(mode.allowedOrigins().isEmpty());
    }

    @Test
    void devModeUsesTheConfiguredVitePortAndOnlyItsLoopbackOrigins() {
        System.setProperty(LocalWebShellDevMode.ENABLED_PROPERTY, "true");
        System.setProperty(LocalWebShellDevMode.PORT_PROPERTY, "5180");

        LocalWebShellDevMode mode = LocalWebShellDevMode.fromSystemProperties();

        assertTrue(mode.enabled());
        assertEquals("http://127.0.0.1:5180", mode.frontendUrl());
        assertEquals(java.util.Set.of("http://127.0.0.1:5180", "http://localhost:5180"), mode.allowedOrigins());
    }

    @Test
    void localSurfaceSelectsViteOnlyWhenDevelopmentIsExplicitlyEnabledAndClosesIdempotently() {
        try (LocalWebShellSurface bundled = new LocalWebShellSurface()) {
            assertFalse(bundled.development());
            assertTrue(bundled.entryUrl().startsWith(bundled.backendUrl() + "/webshell/"));
        }

        System.setProperty(LocalWebShellDevMode.ENABLED_PROPERTY, "true");
        System.setProperty(LocalWebShellDevMode.PORT_PROPERTY, "5180");
        LocalWebShellSurface development = new LocalWebShellSurface();
        try {
            assertTrue(development.development());
            assertTrue(development.entryUrl().startsWith("http://127.0.0.1:5180/?backend="));
        } finally {
            development.close();
            development.close();
        }
    }
}
