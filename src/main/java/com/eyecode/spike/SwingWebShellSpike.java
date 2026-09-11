package com.eyecode.spike;

import com.eyecode.javafx.web.SwingWebShellNativeUi;
import com.eyecode.javafx.web.SwingWebShellSurface;
import com.eyecode.javafx.web.WebShellAssetServer;
import com.eyecode.javafx.web.WebShellNativeController;
import com.eyecode.javafx.web.WebShellWorkspaceController;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class SwingWebShellSpike {
    private SwingWebShellSurface surface;
    private WebShellAssetServer assetServer;
    private WebShellWorkspaceController workspaceController;
    private JFrame frame;

    public static void main(String[] args) {
        System.out.println("[JCEF-SPIKE] main EDT=" + SwingUtilities.isEventDispatchThread());
        SwingUtilities.invokeLater(() -> {
            System.out.println("[JCEF-SPIKE] launch EDT=" + SwingUtilities.isEventDispatchThread());
            new SwingWebShellSpike().launch();
        });
    }

    private void launch() {
        try {
            frame = new JFrame("EyeCode Swing/JCEF Spike");
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            assetServer = WebShellAssetServer.start();
            surface = new SwingWebShellSurface(assetServer.entryUrl());
            SwingWebShellNativeUi nativeUi = new SwingWebShellNativeUi(frame);
            workspaceController = new WebShellWorkspaceController(surface, target -> { }, nativeUi);
            new WebShellNativeController(surface, nativeUi);
            surface.start();
            frame.add(surface.component(), BorderLayout.CENTER);
            frame.setSize(1440, 900);
            frame.setLocationByPlatform(true);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent event) {
                    dispose();
                }

                @Override
                public void windowClosed(WindowEvent event) {
                    dispose();
                }
            });
            frame.setVisible(true);
            System.out.println("[JCEF-SPIKE] frame visible EDT=" + SwingUtilities.isEventDispatchThread());
            surface.logComponentState(frame);
        } catch (Throwable failure) {
            dispose();
            throw new IllegalStateException("Unable to launch the Swing/JCEF WebShell spike", failure);
        }
    }

    private void dispose() {
        JFrame currentFrame = frame;
        frame = null;
        if (currentFrame != null) currentFrame.dispose();
        WebShellWorkspaceController currentWorkspaceController = workspaceController;
        workspaceController = null;
        if (currentWorkspaceController != null) currentWorkspaceController.dispose();
        SwingWebShellSurface currentSurface = surface;
        surface = null;
        if (currentSurface != null) currentSurface.dispose();
        WebShellAssetServer currentAssetServer = assetServer;
        assetServer = null;
        if (currentAssetServer != null) currentAssetServer.close();
    }
}
