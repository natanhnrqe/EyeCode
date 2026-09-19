package com.eyecode.ui.web;

import java.util.concurrent.atomic.AtomicBoolean;

final class LocalWebShellRuntime implements AutoCloseable {
    private final LocalWebShellSurface surface;
    private final WebShellWorkspaceRuntime workspace;
    private final AtomicBoolean closed = new AtomicBoolean();

    LocalWebShellRuntime() {
        surface = new LocalWebShellSurface();
        workspace = WebShellWorkspaceComposition.create(surface);
    }

    LocalWebShellSurface surface() {
        return surface;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        System.out.println("[EyeCode] Shutting down...");
        workspace.close();
        surface.close();
        System.out.println("[EyeCode] WebShell stopped");
    }
}
