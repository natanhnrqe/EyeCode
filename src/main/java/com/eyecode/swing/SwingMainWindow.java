package com.eyecode.swing;

import com.eyecode.javafx.web.SwingWebShellNativeUi;
import com.eyecode.javafx.web.SwingWebShellSurface;
import com.eyecode.javafx.web.WebShellAssetServer;
import com.eyecode.javafx.web.WebShellNativeController;
import com.eyecode.javafx.web.WebShellWorkspaceController;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public final class SwingMainWindow {
    private JFrame frame;
    private WebShellAssetServer assetServer;
    private SwingWebShellSurface surface;
    private WebShellWorkspaceController workspaceController;
    private boolean disposed;

    public void show() {
        frame = new JFrame("EyeCode");
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(1000, 600));
        assetServer = WebShellAssetServer.start();
        surface = new SwingWebShellSurface(assetServer.entryUrl());
        SwingWebShellNativeUi nativeUi = new SwingWebShellNativeUi(frame);
        workspaceController = new WebShellWorkspaceController(surface, target -> { }, nativeUi);
        new WebShellNativeController(surface, nativeUi);
        surface.start();
        frame.add(surface.component(), BorderLayout.CENTER);
        new SwingWindowResizeController(frame, surface.component());
        frame.addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent event) { dispose(); }
            @Override public void windowClosed(WindowEvent event) { dispose(); }
        });
        Rectangle bounds = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration().getBounds();
        frame.setBounds(bounds);
        frame.setVisible(true);
    }

    private void dispose() {
        if (disposed) return;
        disposed = true;
        JFrame currentFrame = frame;
        frame = null;
        if (currentFrame != null && currentFrame.isDisplayable()) currentFrame.dispose();
        WebShellWorkspaceController currentWorkspace = workspaceController;
        workspaceController = null;
        if (currentWorkspace != null) currentWorkspace.dispose();
        SwingWebShellSurface currentSurface = surface;
        surface = null;
        if (currentSurface != null) currentSurface.dispose();
        WebShellAssetServer currentServer = assetServer;
        assetServer = null;
        if (currentServer != null) currentServer.close();
        System.exit(0);
    }
}
