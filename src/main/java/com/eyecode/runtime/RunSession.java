package com.eyecode.runtime;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

public final class RunSession {

    public interface Listener {
        void onOutput(String line, boolean error);
        void onFinished(int exitCode, boolean stopped);
    }

    private final ResolvedExecution execution;
    private final Path workingDirectory;
    private final Listener listener;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> daemon("eyecode-run-stream", r));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> daemon("eyecode-run-stop", r));
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();
    private volatile Process process;

    RunSession(ResolvedExecution execution, Path workingDirectory, Listener listener) {
        this.execution = execution;
        this.workingDirectory = workingDirectory;
        this.listener = listener;
    }

    public void start() {
        executor.submit(this::execute);
    }

    public boolean isRunning() {
        return !finished.get();
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        Process current = process;
        if (current != null && current.isAlive()) {
            destroyTree(current, false);
            scheduler.schedule(() -> {
                if (current.isAlive()) {
                    destroyTree(current, true);
                }
            }, 500, TimeUnit.MILLISECONDS);
        }
    }

    public void dispose() {
        stop();
        executor.shutdownNow();
        scheduler.shutdownNow();
    }

    private void execute() {
        int exitCode = -1;
        try {
            for (List<String> command : execution.commands()) {
                if (stopped.get()) {
                    break;
                }
                ProcessBuilder builder = new ProcessBuilder(command);
                builder.directory(workingDirectory.toFile());
                Process started = builder.start();
                process = started;
                Future<?> outputTask = executor.submit(() -> stream(started.getInputStream(), false));
                Future<?> errorTask = executor.submit(() -> stream(started.getErrorStream(), true));
                exitCode = started.waitFor();
                awaitStream(outputTask);
                awaitStream(errorTask);
                process = null;
                if (exitCode != 0) {
                    break;
                }
            }
            if (stopped.get()) {
                exitCode = -1;
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            exitCode = -1;
        } catch (IOException exception) {
            listener.onOutput(exception.getMessage() == null ? exception.toString() : exception.getMessage(), true);
            exitCode = -1;
        } finally {
            if (finished.compareAndSet(false, true)) {
                listener.onFinished(exitCode, stopped.get());
            }
            executor.shutdown();
        }
    }

    private void stream(InputStream stream, boolean error) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                listener.onOutput(line, error);
            }
        } catch (IOException ignored) {
        }
    }

    private void awaitStream(Future<?> task) throws InterruptedException {
        try {
            task.get(2, TimeUnit.SECONDS);
        } catch (ExecutionException | TimeoutException exception) {
            task.cancel(true);
        }
    }

    private void destroyTree(Process current, boolean forcibly) {
        ProcessHandle handle = current.toHandle();
        handle.descendants().forEach(child -> destroy(child, forcibly));
        destroy(handle, forcibly);
    }

    private void destroy(ProcessHandle handle, boolean forcibly) {
        if (forcibly) {
            handle.destroyForcibly();
        } else {
            handle.destroy();
        }
    }

    private static Thread daemon(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }
}
