package com.eyecode;

import com.eyecode.chromium.ChromiumDemoFrame;
import com.formdev.flatlaf.FlatDarkLaf;

import org.cef.CefApp;
import org.cef.CefSettings;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.io.File;

public class Main {
    public static void main(String[] args) {
        String javaHome = System.getProperty("java.home");

        if (Boolean.getBoolean("eyecode.system.laf")) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception e) {
                FlatDarkLaf.setup();
            }
        } else {
            FlatDarkLaf.setup();
        }

        if (Boolean.getBoolean("eyecode.swing.only")) {
            SwingUtilities.invokeLater(() -> ChromiumDemoFrame.main(new String[]{"--swing-only"}));
            return;
        }

        String[] cefArgs = {"--no-sandbox"};

        if (!CefApp.startup(cefArgs)) {
            System.err.println("JCEF startup() failed");
            System.exit(1);
        }

        CefSettings settings = new CefSettings();
        settings.windowless_rendering_enabled = false;
        settings.browser_subprocess_path = javaHome + "/bin/jcef_helper.exe";
        settings.log_file = new File(System.getProperty("java.io.tmpdir"), "cef_debug.log").getAbsolutePath();
        settings.log_severity = CefSettings.LogSeverity.LOGSEVERITY_VERBOSE;
        settings.cache_path = new File(System.getProperty("java.io.tmpdir"), "eyecode-jcef-cache").getAbsolutePath();
        new File(settings.cache_path).mkdirs();

        CefApp cefApp = CefApp.getInstance(cefArgs, settings);

        cefApp.onInitialization(state -> {
            if (state == CefApp.CefAppState.INITIALIZED) {
                SwingUtilities.invokeLater(() -> new ChromiumDemoFrame(cefApp));
            }
        });
    }
}
