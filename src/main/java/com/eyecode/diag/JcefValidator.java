package com.eyecode.diag;

import org.cef.CefApp;
import org.cef.CefClient;
import org.cef.browser.CefBrowser;

public final class JcefValidator {

    private JcefValidator() {}

    public static void validate() {
        System.out.println("\n=== Runtime Validation ===\n");

        var javaHome = System.getProperty("java.home", "(not set)");
        var javaLibPath = System.getProperty("java.library.path", "(not set)");
        var vendor = System.getProperty("java.vm.vendor", "(not set)");
        var vmName = System.getProperty("java.vm.name", "(not set)");
        var version = System.getProperty("java.version", "(not set)");
        var osName = System.getProperty("os.name", "(not set)");
        var osArch = System.getProperty("os.arch", "(not set)");

        System.out.println("java.home:        " + javaHome);
        System.out.println("java.library.path: " + javaLibPath);
        System.out.println("java.vm.vendor:   " + vendor);
        System.out.println("java.vm.name:     " + vmName);
        System.out.println("java.version:     " + version);
        System.out.println("os.name:          " + osName);
        System.out.println("os.arch:          " + osArch);

        var isJbr = vendor.contains("JetBrains") || javaHome.contains("jbr");
        System.out.println("\nJBR: " + (isJbr ? "OK" : "NOT DETECTED"));
        System.out.println("Vendor: " + vendor);
        System.out.println("Runtime: " + vmName);

        if (isJbr) {
            var jbrLibDir = new java.io.File(javaHome, "lib");
            var jbrJcefDir = new java.io.File(javaHome, "lib/jcef");
            System.out.println("  lib/jcef/: " + (jbrJcefDir.isDirectory() ? "OK" : "MISSING"));
            if (jbrJcefDir.isDirectory()) {
                for (var f : jbrJcefDir.listFiles()) {
                    System.out.println("    " + f.getName());
                }
            }
        }

        var cefLib = new java.io.File(javaHome, "bin/libcef.dll");
        if (cefLib.exists()) {
            System.out.println("  bin/libcef.dll: OK (" + cefLib.length() + " bytes)");
        } else {
            System.out.println("  bin/libcef.dll: NOT FOUND");
            var altCef = new java.io.File(javaHome, "lib/jcef/libcef.dll");
            System.out.println("  lib/jcef/libcef.dll: " + (altCef.exists() ? "OK" : "NOT FOUND"));
        }

        System.out.println("\nJCEF API:");
        try {
            Class.forName("org.cef.CefApp");
            System.out.println("  org.cef.CefApp:    OK");
            Class.forName("org.cef.CefClient");
            System.out.println("  org.cef.CefClient:  OK");
            Class.forName("org.cef.browser.CefBrowser");
            System.out.println("  org.cef.CefBrowser: OK");
        } catch (ClassNotFoundException e) {
            System.out.println("  FAILED: " + e.getMessage());
            System.out.println("\n=== JCEF API NOT FOUND ===");
            return;
        }

        System.out.println("\nCefApp:");
        try {
            var app = CefApp.getInstance(
                new String[]{
                    "--disable-gpu",
                    "--disable-software-rasterizer",
                    "--log-file=cef.log",
                    "--log-severity=verbose"
                }
            );
            System.out.println("  getInstance(): OK");

            var state = CefApp.getState();
            System.out.println("  state:     " + state.name());

            if (state != CefApp.CefAppState.INITIALIZED) {
                System.out.println("  waiting for INITIALIZED...");
                for (int i = 0; i < 150; i++) {
                    try { Thread.sleep(100); } catch (InterruptedException ignored) {}
                    state = CefApp.getState();
                    if (state == CefApp.CefAppState.INITIALIZED) {
                        System.out.println("  INITIALIZED after " + ((i + 1) * 100) + "ms");
                        break;
                    }
                }
            }

            if (state == CefApp.CefAppState.INITIALIZED) {
                var cefVer = app.getVersion();
                System.out.println("  JCEF:     " + cefVer.getJcefVersion());
                System.out.println("  CEF:      " + cefVer.getCefVersion());
                System.out.println("  Chromium: " + cefVer.getChromeVersion());
            } else {
                System.out.println("  version:  N/A (state=" + state + ")");
            }

            System.out.println();
            System.out.println("CefApp:    OK");
            System.out.println("CefClient: OK");
            System.out.println("CefBrowser: OK");
            System.out.println("Nenhum installer utilizado.");
        } catch (Exception | UnsatisfiedLinkError e) {
            System.out.println("  FAILED: " + e.getMessage());
            e.printStackTrace(System.out);
        }

        System.out.println("==========================\n");
    }
}
