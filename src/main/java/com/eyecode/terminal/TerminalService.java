package com.eyecode.terminal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;

public final class TerminalService {

    public interface Listener {
        void onStarted(Path workingDirectory);
        void onOutput(String text, boolean error);
        void onFinished(int exitCode, boolean stopped);
    }

    public record Status(boolean requested, boolean running, String workingDirectory, String endpoint,
                         String sessionId) {
    }

    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Function<Path, List<String>> commandFactory;
    private TerminalSession activeSession;
    private TerminalWebSocketTransport transport;
    private Path workspaceDirectory;
    private Path pendingWorkingDirectory;
    private Path workingDirectory;
    private boolean terminalRequested;
    private boolean disposed;
    private long nextSessionId;

    public TerminalService() {
        this(directory -> defaultShellCommand());
    }

    TerminalService(Function<Path, List<String>> commandFactory) {
        this.commandFactory = commandFactory;
    }

    public synchronized Status show() {
        terminalRequested = true;
        if (workspaceDirectory != null) {
            start(workspaceDirectory);
        }
        return status();
    }

    public synchronized void hide() {
        terminalRequested = false;
    }

    public synchronized void setWorkspaceDirectory(Path directory) {
        Path normalized = normalize(directory);
        if (java.util.Objects.equals(workspaceDirectory, normalized)) {
            return;
        }
        lifecycle("workspace-switch", "", workspaceDirectory, normalized);
        workspaceDirectory = normalized;
        pendingWorkingDirectory = normalized;
        workingDirectory = null;
        closeTransport();
        if (activeSession != null && activeSession.isRunning()) {
            stopActiveSession();
        } else if (terminalRequested && normalized != null) {
            pendingWorkingDirectory = null;
            launch(normalized);
        } else if (normalized == null) {
            pendingWorkingDirectory = null;
            workingDirectory = null;
        }
    }

    public synchronized boolean start(Path directory) {
        Path normalized = normalize(directory);
        if (disposed || normalized == null) {
            return false;
        }
        workspaceDirectory = normalized;
        if (activeSession != null && activeSession.isRunning()) {
            if (normalized.equals(workingDirectory)) {
                return true;
            }
            pendingWorkingDirectory = normalized;
            closeTransport();
            stopActiveSession();
            return true;
        }
        pendingWorkingDirectory = null;
        launch(normalized);
        return true;
    }

    public synchronized boolean restart() {
        terminalRequested = true;
        return restart(workspaceDirectory);
    }

    public synchronized boolean restart(Path directory) {
        Path normalized = normalize(directory);
        if (disposed || normalized == null) {
            return false;
        }
        workspaceDirectory = normalized;
        if (activeSession != null && activeSession.isRunning()) {
            pendingWorkingDirectory = normalized;
            closeTransport();
            stopActiveSession();
            return true;
        }
        pendingWorkingDirectory = null;
        launch(normalized);
        return true;
    }

    public synchronized boolean send(byte[] bytes) {
        return activeSession != null && activeSession.send(bytes);
    }

    public synchronized void resize(int columns, int rows) {
        if (activeSession != null) {
            activeSession.resize(columns, rows);
        }
    }

    public synchronized boolean stop() {
        terminalRequested = false;
        pendingWorkingDirectory = null;
        closeTransport();
        if (activeSession == null || !activeSession.isRunning()) {
            return false;
        }
        stopActiveSession();
        return true;
    }

    public synchronized boolean isRunning() {
        return activeSession != null && activeSession.isRunning();
    }

    public synchronized Path workingDirectory() {
        return workingDirectory;
    }

    public synchronized Status status() {
        String sessionId = activeSession == null ? "" : activeSession.sessionId();
        Status status = new Status(terminalRequested, isRunning(),
                workingDirectory == null ? "" : workingDirectory.toString(),
                transport == null ? "" : transport.endpoint(), sessionId);
        lifecycle("status", sessionId, workingDirectory, null, status.endpoint());
        return status;
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
        terminalRequested = false;
        pendingWorkingDirectory = null;
        if (activeSession != null) {
            activeSession.dispose();
        }
        activeSession = null;
        workspaceDirectory = null;
        workingDirectory = null;
        closeTransport();
        listeners.clear();
    }

    private void launch(Path directory) {
        if (disposed) {
            return;
        }
        String sessionId = Long.toString(++nextSessionId);
        try {
            ensureTransport(sessionId);
        } catch (IllegalStateException exception) {
            return;
        }
        workingDirectory = directory;
        TerminalSession[] holder = new TerminalSession[1];
        TerminalSession session = new TerminalSession(sessionId, directory, commandFactory.apply(directory), new TerminalSession.Listener() {
            @Override
            public void onStarted(Path startedDirectory) {
                for (Listener listener : listeners) {
                    listener.onStarted(startedDirectory);
                }
            }

            @Override
            public void onOutput(byte[] bytes) {
                TerminalWebSocketTransport current;
                synchronized (TerminalService.this) {
                    current = transport;
                }
                if (current != null) {
                    current.send(bytes);
                }
            }

            @Override
            public void onFinished(int exitCode, boolean stopped) {
                finish(holder[0], exitCode, stopped);
            }
        });
        holder[0] = session;
        activeSession = session;
        lifecycle("create", sessionId, directory, null);
        session.start();
    }

    private synchronized void finish(TerminalSession session, int exitCode, boolean stopped) {
        if (activeSession != session) {
            return;
        }
        activeSession = null;
        lifecycle("process-exit", session.sessionId(), workingDirectory, null);
        Path next = pendingWorkingDirectory;
        pendingWorkingDirectory = null;
        if (!disposed && terminalRequested && next != null) {
            launch(next);
        } else if (!isRunning()) {
            workingDirectory = null;
            closeTransport();
        }
        for (Listener listener : listeners) {
            listener.onFinished(exitCode, stopped);
        }
    }

    private void ensureTransport(String sessionId) {
        if (transport != null) {
            return;
        }
        byte[] tokenBytes = new byte[32];
        new SecureRandom().nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
        transport = TerminalWebSocketTransport.start(token, this::send,
                () -> lifecycle("connect", sessionId, workingDirectory, null));
    }

    private void stopActiveSession() {
        TerminalSession session = activeSession;
        if (session == null) {
            return;
        }
        lifecycle("stop", session.sessionId(), workingDirectory, null);
        session.stop();
    }

    private void lifecycle(String event, String sessionId, Path workspace, Path nextWorkspace) {
        lifecycle(event, sessionId, workspace, nextWorkspace, "");
    }

    private void lifecycle(String event, String sessionId, Path workspace, Path nextWorkspace, String endpoint) {
        StringBuilder message = new StringBuilder("TERMINAL_LIFECYCLE ").append(event);
        if ("workspace-switch".equals(event)) {
            message.append(" old=").append(workspace == null ? "" : workspace);
            message.append(" new=").append(nextWorkspace == null ? "" : nextWorkspace);
        } else if (workspace != null) {
            message.append(" workspace=").append(workspace);
        }
        if (sessionId != null && !sessionId.isBlank()) {
            message.append(" session=").append(sessionId);
        }
        if (endpoint != null && !endpoint.isBlank()) {
            message.append(" endpoint=").append(endpoint);
        }
        System.out.println(message);
    }

    private void closeTransport() {
        TerminalWebSocketTransport current = transport;
        transport = null;
        if (current != null) {
            current.close();
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