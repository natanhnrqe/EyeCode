package com.eyecode.javafx.web;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

public final class LocalWebShellLauncher {
    private LocalWebShellLauncher() {
    }

    public static void main(String[] args) {
        LocalWebShellSurface surface = new LocalWebShellSurface();
        WebShellWorkspaceController workspace = new WebShellWorkspaceController(surface,
                target -> { }, new LocalWebShellNativeUi());
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            workspace.dispose();
            surface.close();
        }, "eyecode-local-webshell-shutdown"));
        openBrowser(surface.entryUrl());
        System.out.println("[LOCAL-WEBSHELL] opened " + surface.entryUrl());
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
