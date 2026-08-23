package com.eyecode.javafx.ui.toolwindow.content;

import com.eyecode.runtime.RunRequest;
import com.eyecode.runtime.RunService;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

public final class RunToolWindowContent extends VBox implements RunService.Listener {

    private final Label status = new Label("No run started");
    private final TextArea output = new TextArea();

    public RunToolWindowContent(RunService runService) {
        getStyleClass().add("run-tool-window-content");
        setPadding(new Insets(8));
        setSpacing(6);
        status.getStyleClass().add("run-status");
        output.setEditable(false);
        output.setWrapText(false);
        output.getStyleClass().add("run-output");
        VBox.setVgrow(output, Priority.ALWAYS);
        getChildren().addAll(status, output);
        if (runService != null) {
            runService.addListener(this);
        }
    }

    @Override
    public void onStarted(RunRequest request) {
        onFx(() -> status.setText("Running " + request.configurationLabel()));
    }

    @Override
    public void onOutput(String line, boolean error) {
        onFx(() -> {
            if (line == null) {
                output.clear();
            } else {
                output.appendText((error ? "[stderr] " : "") + line + System.lineSeparator());
            }
        });
    }

    @Override
    public void onFinished(int exitCode, boolean stopped) {
        onFx(() -> status.setText(stopped
                ? "Process stopped"
                : "Process finished with exit code " + exitCode));
    }

    TextArea outputForTest() {
        return output;
    }

    Label statusForTest() {
        return status;
    }

    private void onFx(Runnable action) {
        if (Platform.isFxApplicationThread()) {
            action.run();
        } else {
            Platform.runLater(action);
        }
    }
}
