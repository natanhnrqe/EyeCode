package com.eyecode.terminal;

import com.eyecode.runtime.ProcessTree;
import com.pty4j.PtyProcess;
import com.pty4j.PtyProcessBuilder;
import com.pty4j.WinSize;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
        void onOutput(byte[] bytes);
        void onFinished(int exitCode, boolean stopped);
    }

    private final Path workingDirectory;
    private final List<String> command;
    private final Listener listener;
    private final ExecutorService executor = Executors.newSingleThreadExecutor(r -> daemon("eyecode-terminal-session", r));
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> daemon("eyecode-terminal-stop", r));
    private final AtomicBoolean stopped = new AtomicBoolean();
    private final AtomicBoolean finished = new AtomicBoolean();
    private final Object inputLock = new Object();
    private volatile PtyProcess process;
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
        PtyProcess current = process;
        return !finished.get() && (current == null || current.isRunning());
    }

    boolean send(byte[] bytes) {
        OutputStream current = input;
        if (bytes == null || bytes.length == 0 || current == null || finished.get()) {
            return false;
        }
        synchronized (inputLock) {
            try {
                current.write(bytes);
                current.flush();
                return true;
            } catch (IOException ignored) {
                return false;
            }
        }
    }

    void resize(int columns, int rows) {
        PtyProcess current = process;
        if (current == null || !current.isRunning() || columns <= 0 || rows <= 0) {
            return;
        }
        current.setWinSize(new WinSize(columns, rows));
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
            PtyProcess started = new PtyProcessBuilder(command.toArray(String[]::new))
                    .setEnvironment(System.getenv())
                    .setDirectory(workingDirectory.toString())
                    .start();
            process = started;
            input = started.getOutputStream();
            listener.onStarted(workingDirectory);
            stream(started.getInputStream());
            exitCode = started.waitFor();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        } catch (IOException ignored) {
        } finally {
            input = null;
            process = null;
            if (finished.compareAndSet(false, true)) {
                listener.onFinished(stopped.get() ? -1 : exitCode, stopped.get());
            }
            executor.shutdown();
        }
    }

    private void stream(InputStream source) {
        try (InputStream inputStream = source) {
            byte[] buffer = new byte[4096];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                if (read > 0) {
                    byte[] output = java.util.Arrays.copyOf(buffer, read);
                    listener.onOutput(output);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static Thread daemon(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }
}