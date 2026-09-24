package com.eyecode.swing;

import com.eyecode.ui.web.SwingWebShellNativeUi;
import com.eyecode.ui.web.SwingWebShellSurface;
import com.eyecode.ui.web.WebShellAssetServer;
import com.eyecode.ui.web.WebShellNativeController;
import com.eyecode.ui.web.WebShellWorkspaceComposition;
import com.eyecode.ui.web.WebShellWorkspaceRuntime;

import javax.swing.JFrame;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Path;

public final class SwingMainWindow {
    private JFrame frame;
    private WebShellAssetServer assetServer;
    private SwingWebShellSurface surface;
    private WebShellWorkspaceRuntime workspaceController;
    private boolean disposed;
    private final Path startupProject;

    public SwingMainWindow() {
        this(null);
    }

    public SwingMainWindow(Path startupProject) {
        this.startupProject = startupProject;
    }

    public void show() {
        frame = new JFrame("EyeCode");
        frame.setUndecorated(true);
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setMinimumSize(new Dimension(1000, 600));
        assetServer = WebShellAssetServer.start();
        surface = new SwingWebShellSurface(assetServer.entryUrl());
        SwingWebShellNativeUi nativeUi = new SwingWebShellNativeUi(frame);
        workspaceController = WebShellWorkspaceComposition.create(surface, target -> { }, nativeUi);
        if (startupProject != null) workspaceController.openProjectAtStartup(startupProject);
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
        WebShellWorkspaceRuntime currentWorkspace = workspaceController;
        workspaceController = null;
        if (currentWorkspace != null) currentWorkspace.close();
        SwingWebShellSurface currentSurface = surface;
        surface = null;
        if (currentSurface != null) currentSurface.dispose();
        WebShellAssetServer currentServer = assetServer;
        assetServer = null;
        if (currentServer != null) currentServer.close();
        System.exit(0);
    }
}
