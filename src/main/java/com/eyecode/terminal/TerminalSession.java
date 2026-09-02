package com.eyecode.terminal;

import com.eyecode.runtime.ProcessTree;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

final class TerminalSession {

    interface Listener {
        void onStarted(Path workingDirectory);
        void onOutput(String text, boolean error);
        void onFinished(int exitCode, boolean stopped);
    }

    private final Path workingDirectory;
    private final List<String> command;
    private final Listener listener;
    private final ExecutorService executor = Executors.newCachedThreadPool(r -> daemon("eyecode-terminal-stream", r));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> daemon("eyecode-terminal-stop", r));
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();
    private final Object inputLock = new Object();
    private volatile Process process;
    private volatile OutputStream input;

    TerminalSession(Path workingDirectory, List<String> command, Listener listener) {
        this.workingDirectory = workingDirectory;
        this.command = List.copyOf(command);
        this.listener = listener;
    }

    void start() {
        executor.submit(this::execute);
    }

    boolean isRunning() {
        return !finished.get();
    }

    boolean send(String text) {
        OutputStream current = input;
        if (text == null || current == null || finished.get()) {
            return false;
        }
        synchronized (inputLock) {
            try {
                current.write(text.getBytes(StandardCharsets.UTF_8));
                current.flush();
                return true;
            } catch (IOException exception) {
                listener.onOutput(exception.getMessage() == null ? exception.toString() : exception.getMessage(), true);
                return false;
            }
        }
    }

    void stop() {
        if (!stopped.compareAndSet(false, true)) {
            return;
        }
        Process current = process;
        if (current != null && current.isAlive()) {
            ProcessTree.destroy(current, false);
            scheduler.schedule(() -> {
                if (current.isAlive()) {
                    ProcessTree.destroy(current, true);
                }
            }, 500, TimeUnit.MILLISECONDS);
        }
    }

    void dispose() {
        stop();
        executor.shutdownNow();
        scheduler.shutdownNow();
    }

    private void execute() {
        int exitCode = -1;
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
            builder.directory(workingDirectory.toFile());
            Process started = builder.start();
            process = started;
            input = started.getOutputStream();
            listener.onStarted(workingDirectory);
            executor.submit(() -> stream(started.getInputStream(), false));
            executor.submit(() -> stream(started.getErrorStream(), true));
            exitCode = started.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException exception) {
            listener.onOutput(exception.getMessage() == null ? exception.toString() : exception.getMessage(), true);
        } finally {
            input = null;
            process = null;
            if (finished.compareAndSet(false, true)) {
                listener.onFinished(stopped.get() ? -1 : exitCode, stopped.get());
            }
            executor.shutdown();
        }
    }

    private void stream(InputStream source, boolean error) {
        try (InputStream inputStream = source) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read > 0) {
                    listener.onOutput(new String(buffer, 0, read, StandardCharsets.UTF_8), error);
                }
            }
        } catch (IOException exception) {
            if (!finished.get()) {
                listener.onOutput(exception.getMessage() == null ? exception.toString() : exception.getMessage(), true);
            }
        }
    }

    private static Thread daemon(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }
}
