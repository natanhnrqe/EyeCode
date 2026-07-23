package com.eyecode.browser;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.CefSettings;

import java.io.File;

public final class BrowserManager {

    private static volatile BrowserManager instance;

    private final CefApp cefApp;
    private final CefClient cefClient;

    private BrowserManager() {
        String[] cefArgs = {"--no-sandbox"};

        if (!CefApp.startup(cefArgs)) {
            throw new IllegalStateException("JCEF startup() failed");
        }

        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = false;
        settings.browser_subprocess_path = System.getProperty("java.home") + "/bin/jcef_helper.exe";
        settings.log_file = new File(System.getProperty("java.io.tmpdir"), "eyecode-cef.log").getAbsolutePath();
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_WARNING;
        settings.cache_path = new File(System.getProperty("java.io.tmpdir"), "eyecode-jcef-cache").getAbsolutePath();
        new File(settings.cache_path).mkdirs();

        cefApp = CefApp.getInstance(cefArgs, settings);
        cefClient = cefApp.createClient();
    }

    public static BrowserManager getInstance() {
        if (instance == null) {
            synchronized (BrowserManager.class) {
                if (instance == null) {
                    instance = new BrowserManager();
                }
            }
        }
        return instance;
    }

    public CefApp getApp() {
        return cefApp;
    }

    public CefClient createClient() {
        return cefApp.createClient();
    }

    @Deprecated(forRemoval = true)
    public CefClient getClient() {
        return cefClient;
    }

    public static boolean isInitialized() {
        return instance != null;
    }

    public void dispose() {
        cefClient.dispose();
        cefApp.dispose();
    }
}
