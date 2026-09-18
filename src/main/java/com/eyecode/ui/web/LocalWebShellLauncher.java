package com.eyecode.ui.web;

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
            if (Boolean.getBoolean("eyecode.web.openBrowser")) {
                LocalWebShellBrowserOpener.open(surface.entryUrl());
            }
        } catch (RuntimeException exception) {
            runtime.close();
            throw exception;
        }
    }

}
