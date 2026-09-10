package com.eyecode.javafx.web;

import javafx.application.Platform;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class JavaFxWebShellNativeUi implements WebShellNativeUi {
    private final Stage stage;

    public JavaFxWebShellNativeUi(Stage stage) {
        this.stage = stage;
    }

    @Override
    public Path chooseDirectory(String title) {
        return callOnFxThread(() -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle(title);
            File selected = chooser.showDialog(stage);
            return selected == null ? null : selected.toPath().toAbsolutePath().normalize();
        });
    }

    @Override
    public Path chooseJavaSaveTarget(String suggestedName) {
        return callOnFxThread(() -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Save Java File");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Java Files", "*.java"));
            if (suggestedName != null && !suggestedName.isBlank()) chooser.setInitialFileName(suggestedName);
            File selected = chooser.showSaveDialog(stage);
            return selected == null ? null : selected.toPath().toAbsolutePath().normalize();
        });
    }

    @Override
    public void minimizeWindow() {
        Platform.runLater(() -> stage.setIconified(true));
    }

    @Override
    public void toggleMaximizeWindow() {
        Platform.runLater(() -> stage.setMaximized(!stage.isMaximized()));
    }

    @Override
    public void closeWindow() {
        Platform.runLater(stage::close);
    }

    private static <T> T callOnFxThread(Callable<T> task) {
        if (Platform.isFxApplicationThread()) return call(task);
        CompletableFuture<T> result = new CompletableFuture<>();
        Platform.runLater(() -> {
            try {
                result.complete(task.call());
            } catch (Exception exception) {
                result.completeExceptionally(exception);
            }
        });
        try {
            return result.get();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return null;
        } catch (ExecutionException | IllegalStateException exception) {
            return null;
        }
    }

    private static <T> T call(Callable<T> task) {
        try {
            return task.call();
        } catch (Exception exception) {
            return null;
        }
    }
}
