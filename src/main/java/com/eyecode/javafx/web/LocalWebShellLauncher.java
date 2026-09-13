package com.eyecode.javafx.web;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

public final class LocalWebShellLauncher {
    private LocalWebShellLauncher() {
    }

    public static void main(String[] args) {
        LocalWebShellRuntime runtime = new LocalWebShellRuntime();
        Runtime.getRuntime().addShutdownHook(new Thread(runtime::close, "eyecode-local-webshell-shutdown"));
        LocalWebShellSurface surface = runtime.surface();
        try {
            if (surface.development()) {
                System.out.println("[EyeCode] WebShell DEV mode enabled");
                System.out.println("[EyeCode] Frontend: " + surface.entryUrl().replaceFirst("/\\?backend=.*$", ""));
                System.out.println("[EyeCode] Backend: " + surface.backendUrl());
            } else {
                System.out.println("[EyeCode] WebShell bundled mode");
                System.out.println("[EyeCode] URL: " + surface.entryUrl());
            }
            openBrowser(surface.entryUrl());
        } catch (RuntimeException exception) {
            runtime.close();
            throw exception;
        }
    }

    private static void openBrowser(String url) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                throw new IllegalStateException("Desktop browser integration is unavailable");
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException exception) {
            throw new IllegalStateException("Unable to open the default browser", exception);
        }
    }
}
