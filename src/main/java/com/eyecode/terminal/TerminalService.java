package com.eyecode.terminal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class TerminalService {

    public interface Listener {
        void onStarted(Path workingDirectory);
        void onOutput(String text, boolean error);
        void onFinished(int exitCode, boolean stopped);
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Function<Path, List<String>> commandFactory;
    private TerminalSession activeSession;
    private Path pendingWorkingDirectory;
    private Path workingDirectory;
    private boolean disposed;

    public TerminalService() {
        this(directory -> defaultShellCommand());
    }

    TerminalService(Function<Path, List<String>> commandFactory) {
        this.commandFactory = commandFactory;
    }

    public synchronized boolean start(Path directory) {
        Path normalized = normalize(directory);
        if (disposed || normalized == null) {
            return false;
        }
        if (activeSession != null && activeSession.isRunning()) {
            if (normalized.equals(workingDirectory)) {
                return true;
            }
            pendingWorkingDirectory = normalized;
            activeSession.stop();
            return true;
        }
        launch(normalized);
        return true;
    }

    public synchronized boolean restart(Path directory) {
        Path normalized = normalize(directory);
        if (disposed || normalized == null) {
            return false;
        }
        if (activeSession != null && activeSession.isRunning()) {
            pendingWorkingDirectory = normalized;
            activeSession.stop();
            return true;
        }
        launch(normalized);
        return true;
    }

    public synchronized boolean send(String text) {
        return activeSession != null && activeSession.send(text);
    }

    public synchronized boolean stop() {
        if (activeSession == null || !activeSession.isRunning()) {
            return false;
        }
        pendingWorkingDirectory = null;
        activeSession.stop();
        return true;
    }

    public synchronized boolean isRunning() {
        return activeSession != null && activeSession.isRunning();
    }

    public synchronized Path workingDirectory() {
        return workingDirectory;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        pendingWorkingDirectory = null;
        if (activeSession != null) {
            activeSession.dispose();
        }
        activeSession = null;
        workingDirectory = null;
        listeners.clear();
    }

    private void launch(Path directory) {
        workingDirectory = directory;
        TerminalSession[] holder = new TerminalSession[1];
        TerminalSession session = new TerminalSession(directory, commandFactory.apply(directory), new TerminalSession.Listener() {
            @Override
            public void onStarted(Path startedDirectory) {
                for (Listener listener : listeners) {
                    listener.onStarted(startedDirectory);
                }
            }

            @Override
            public void onOutput(String text, boolean error) {
                for (Listener listener : listeners) {
                    listener.onOutput(text, error);
                }
            }

            @Override
            public void onFinished(int exitCode, boolean stopped) {
                finish(holder[0], exitCode, stopped);
            }
        });
        holder[0] = session;
        activeSession = session;
        session.start();
    }

    private synchronized void finish(TerminalSession session, int exitCode, boolean stopped) {
        if (activeSession != session) {
            return;
        }
        activeSession = null;
        for (Listener listener : listeners) {
            listener.onFinished(exitCode, stopped);
        }
        Path next = pendingWorkingDirectory;
        pendingWorkingDirectory = null;
        if (!disposed && next != null) {
            launch(next);
        }
    }

    private Path normalize(Path directory) {
        if (directory == null) {
            return null;
        }
        Path normalized = directory.toAbsolutePath().normalize();
        return Files.isDirectory(normalized) ? normalized : null;
    }

    private static List<String> defaultShellCommand() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            return List.of("powershell.exe", "-NoLogo", "-NoExit");
        }
        return List.of("sh", "-l");
    }
}
