package com.eyecode.runtime;

import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.model.ProjectModel;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public final class RunService {

    public interface Listener {
        void onStarted(RunRequest request);
        void onOutput(String line, boolean error);
        void onFinished(int exitCode, boolean stopped);
    }

    private final ProjectLifecycleService lifecycleService;
    private final ProjectExecutionResolver resolver;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private volatile RunSession activeSession;
    private volatile RunRequest lastRequest;
    private volatile boolean rerunAfterStop;
    private volatile boolean disposed;
    private final List<String> outputHistory = new CopyOnWriteArrayList<>();
    private volatile boolean hasCompletion;
    private volatile int lastExitCode;
    private volatile boolean lastStopped;

    public RunService(ProjectLifecycleService lifecycleService) {
        this(lifecycleService, new ProjectExecutionResolver());
    }

    public RunService(ProjectLifecycleService lifecycleService, ProjectExecutionResolver resolver) {
        this.lifecycleService = lifecycleService;
        this.resolver = resolver == null ? new ProjectExecutionResolver() : resolver;
    }

    public synchronized boolean runCurrent() {
        ProjectModel project = lifecycleService == null ? null : lifecycleService.currentProject();
        if (project == null) {
            publishOutput("No project is open.", true);
            return false;
        }
        return run(new RunRequest(project));
    }

    public synchronized boolean run(RunRequest request) {
        if (disposed || request == null || isRunning()) {
            return false;
        }
        ResolvedExecution execution;
        try {
            execution = resolver.resolve(request.project());
        } catch (RuntimeException exception) {
            publishOutput(exception.getMessage() == null ? exception.toString() : exception.getMessage(), true);
            publishFinished(-1, false);
            return false;
        }
        lastRequest = request;
        clearOutput();
        RunSession session = new RunSession(execution, request.project().getRootDir(), new SessionListener());
        activeSession = session;
        for (Listener listener : listeners) {
            listener.onStarted(request);
        }
        session.start();
        return true;
    }

    public synchronized boolean rerun() {
        if (lastRequest == null) {
            return false;
        }
        if (isRunning()) {
            rerunAfterStop = true;
            stop();
            return true;
        }
        return run(lastRequest);
    }

    public synchronized void stop() {
        RunSession session = activeSession;
        if (session != null) {
            session.stop();
        }
    }

    public synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        rerunAfterStop = false;
        RunSession session = activeSession;
        if (session != null) {
            session.dispose();
        }
        activeSession = null;
    }

    public boolean isRunning() {
        RunSession session = activeSession;
        return session != null && session.isRunning();
    }

    public boolean hasLastRequest() {
        return lastRequest != null;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            if (isRunning() && lastRequest != null) {
                listener.onStarted(lastRequest);
            }
            for (String line : outputHistory) {
                listener.onOutput(line, false);
            }
            if (hasCompletion) {
                listener.onFinished(lastExitCode, lastStopped);
            }
        }
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    private void clearOutput() {
        outputHistory.clear();
        hasCompletion = false;
        for (Listener listener : listeners) {
            listener.onOutput(null, false);
        }
    }

    private void publishOutput(String line, boolean error) {
        if (line != null && !line.isEmpty()) {
            outputHistory.add((error ? "[stderr] " : "") + line);
        }
        for (Listener listener : listeners) {
            listener.onOutput(line, error);
        }
    }

    private void publishFinished(int exitCode, boolean stopped) {
        lastExitCode = exitCode;
        lastStopped = stopped;
        hasCompletion = true;
        for (Listener listener : listeners) {
            listener.onFinished(exitCode, stopped);
        }
    }

    private final class SessionListener implements RunSession.Listener {
        @Override
        public void onOutput(String line, boolean error) {
            publishOutput(line, error);
        }

        @Override
        public synchronized void onFinished(int exitCode, boolean stopped) {
            activeSession = null;
            publishFinished(exitCode, stopped);
            if (rerunAfterStop && !disposed) {
                rerunAfterStop = false;
                run(lastRequest);
            }
        }
    }
}
