package com.eyecode.ui.web;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

final class LocalWebShellRuntime implements AutoCloseable {
    private final LocalWebShellSurface surface;
    private final WebShellWorkspaceRuntime workspace;
    private final AtomicBoolean closed = new AtomicBoolean();

    LocalWebShellRuntime() {
        this(null);
    }

    LocalWebShellRuntime(Path startupProject) {
        surface = new LocalWebShellSurface();
        workspace = WebShellWorkspaceComposition.create(surface, target -> { }, new LocalWebShellNativeUi());
        if (startupProject != null) workspace.openProjectAtStartup(startupProject);
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
