package com.eyecode.ui.web;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

final class LocalWebShellBrowserOpener {
    private LocalWebShellBrowserOpener() {
    }

    static void open(String url) {
        try {
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                System.out.println("[EyeCode] Browser opening is unavailable; open " + url);
                return;
            }
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException | RuntimeException exception) {
            System.out.println("[EyeCode] Unable to open browser; open " + url);
        }
    }
}
