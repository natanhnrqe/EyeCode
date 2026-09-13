package com.eyecode.javafx.web;

import java.util.concurrent.atomic.AtomicBoolean;

final class LocalWebShellRuntime implements AutoCloseable {
    private final LocalWebShellSurface surface;
    private final WebShellWorkspaceController workspace;
    private final AtomicBoolean closed = new AtomicBoolean();

    LocalWebShellRuntime() {
        surface = new LocalWebShellSurface();
        workspace = new WebShellWorkspaceController(surface, target -> { }, new LocalWebShellNativeUi());
    }

    LocalWebShellSurface surface() {
        return surface;
    }

    @Override
    public void close() {
        if (!closed.compareAndSet(false, true)) return;
        System.out.println("[EyeCode] Shutting down...");
        workspace.dispose();
        surface.close();
        System.out.println("[EyeCode] WebShell stopped");
    }
}
