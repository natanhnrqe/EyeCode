package com.eyecode.javafx.ceffx;

import com.techsenger.ceffx.core.CefApp;
import com.techsenger.ceffx.core.CefSettings;

public final class CeffxRuntime {

    private static CefApp app;

    private CeffxRuntime() {
    }

    public static synchronized void startup(String[] args) {
        if (app != null) {
            return;
        }
        if (!CefApp.startup(args)) {
            throw new IllegalStateException("CEFFX startup failed");
        }
        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = true;
        settings.multi_threaded_message_loop = true;
        settings.external_message_pump = false;
        settings.command_line_args_disabled = false;
        app = CefApp.getInstance(settings);
    }

    public static synchronized CefApp app() {
        if (app == null) {
            throw new IllegalStateException("CEFFX runtime has not started");
        }
        return app;
    }

    public static void runLater(Runnable task) {
        app();
        CefApp.runLater(task);
    }

    public static synchronized void dispose() {
        CefApp current = app;
        if (current == null) {
            return;
        }
        app = null;
        CefApp.runLater(current::dispose);
    }
}
