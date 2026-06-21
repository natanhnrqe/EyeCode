package com.eyecode.runtime;

import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.BuildOutputEvent;
import com.eyecode.eventbus.events.RuntimeStatusEvent;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.run.RunManager;

import java.io.File;

public class DefaultRuntimeEngine implements RuntimeEngine {

    private final EventBus eventBus;
    private final RunManager runManager;
    private volatile boolean running;
    private volatile boolean stopRequested;
    private volatile ProcessResult lastResult;
    private volatile Thread workerThread;

    public DefaultRuntimeEngine(EventBus eventBus, RunManager runManager) {
        this.eventBus = eventBus;
        this.runManager = runManager;
        this.running = false;
        this.stopRequested = false;
        this.lastResult = new ProcessResult(-1, "", "Not started", 0);
    }

    @Override
    public ProcessResult run(ProjectModel model) {
        if (model == null) {
            return new ProcessResult(-1, "", "Project model is required", 0);
        }
        if (running) {
            return new ProcessResult(-1, "", "Process already running", 0);
        }

        running = true;
        stopRequested = false;
        publishStatus(true, "Running...");

        File projectRoot = model.getRootDir().toFile();
        workerThread = new Thread(() -> execute(projectRoot), "eyecode-runtime-engine");
        workerThread.setDaemon(true);
        workerThread.start();

        return new ProcessResult(0, "", "", 0);
    }

    @Override
    public void stop() {
        if (!running && workerThread == null) return;

        stopRequested = true;
        runManager.stop();
        Thread thread = workerThread;
        if (thread != null) {
            thread.interrupt();
        }
        running = false;
        publishStatus(false, "Stopped");
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    public ProcessResult getLastResult() {
        return lastResult;
    }

    private void execute(File projectRoot) {
        long startedAt = System.currentTimeMillis();
        String output = "";
        String error = "";
        int exitCode = 0;

        try {
            output = runManager.runProject(projectRoot);
            publishOutput(output);
            if (!stopRequested) {
                publishStatus(false, "Finished");
            }
        } catch (Exception ex) {
            error = ex.getMessage() == null ? ex.toString() : ex.getMessage();
            exitCode = -1;
            publishOutput(error);
            if (!stopRequested) {
                publishStatus(false, "Execution failed");
            }
        } finally {
            long durationMs = System.currentTimeMillis() - startedAt;
            lastResult = new ProcessResult(exitCode, output, error, durationMs);
            running = false;
            workerThread = null;
        }
    }

    private void publishOutput(String output) {
        // TODO: stream output in real time after RunManager exposes a line callback API.
        if (eventBus == null || output == null || output.isEmpty()) return;
        for (String line : output.split("\\R")) {
            if (!line.isEmpty()) {
                eventBus.publish(new BuildOutputEvent(line));
            }
        }
    }

    private void publishStatus(boolean running, String statusText) {
        if (eventBus != null) {
            eventBus.publish(new RuntimeStatusEvent(running, statusText));
        }
    }
}
