package com.eyecode.javafx.web;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

public final class SwingWebShellNativeUi implements WebShellNativeUi {
    private final JFrame frame;
    private int dragOffsetX;
    private int dragOffsetY;
    private boolean dragging;

    public SwingWebShellNativeUi(JFrame frame) {
        this.frame = frame;
    }

    @Override
    public Path chooseDirectory(String title) {
        return callOnEdt(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle(title);
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            return chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION
                    ? chooser.getSelectedFile().toPath().toAbsolutePath().normalize() : null;
        });
    }

    @Override
    public Path chooseJavaSaveTarget(String suggestedName) {
        return callOnEdt(() -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Save Java File");
            chooser.setFileFilter(new FileNameExtensionFilter("Java Files", "java"));
            if (suggestedName != null && !suggestedName.isBlank()) chooser.setSelectedFile(new File(suggestedName));
            return chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION
                    ? chooser.getSelectedFile().toPath().toAbsolutePath().normalize() : null;
        });
    }

    @Override
    public void minimizeWindow() {
        SwingUtilities.invokeLater(() -> frame.setState(JFrame.ICONIFIED));
    }

    @Override
    public void toggleMaximizeWindow() {
        SwingUtilities.invokeLater(() -> frame.setExtendedState(
                frame.getExtendedState() == JFrame.MAXIMIZED_BOTH ? JFrame.NORMAL : JFrame.MAXIMIZED_BOTH));
    }

    @Override
    public void closeWindow() {
        SwingUtilities.invokeLater(frame::dispose);
    }

    @Override
    public void beginWindowDrag(int screenX, int screenY) {
        SwingUtilities.invokeLater(() -> {
            dragOffsetX = screenX - frame.getX();
            dragOffsetY = screenY - frame.getY();
            dragging = true;
        });
    }

    @Override
    public void moveWindow(int screenX, int screenY) {
        SwingUtilities.invokeLater(() -> {
            if (dragging && frame.getExtendedState() != JFrame.MAXIMIZED_BOTH) {
                frame.setLocation(screenX - dragOffsetX, screenY - dragOffsetY);
            }
        });
    }

    @Override
    public void endWindowDrag() {
        SwingUtilities.invokeLater(() -> dragging = false);
    }

    private static <T> T callOnEdt(Callable<T> task) {
        if (SwingUtilities.isEventDispatchThread()) return call(task);
        AtomicReference<T> result = new AtomicReference<>();
        try {
            SwingUtilities.invokeAndWait(() -> result.set(call(task)));
        } catch (Exception ignored) {
        }
        return result.get();
    }

    private static <T> T call(Callable<T> task) {
        try {
            return task.call();
        } catch (Exception ignored) {
            return null;
        }
    }
}
