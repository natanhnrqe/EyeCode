package com.eyecode.runtime;

import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.model.ProjectModel;

import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;

public final class RunService {

    @FunctionalInterface
    interface ExecutionResolver {
        ResolvedExecution resolve(ProjectModel project, RunConfiguration configuration);
    }

    public interface Listener {
        default void onPhase(RunPhase phase) { }
        void onStarted(RunRequest request);
        void onOutput(String text, boolean error);
        void onFinished(int exitCode, boolean stopped);
    }

    private final ProjectLifecycleService lifecycleService;
    private final ProjectLifecycleService.Listener lifecycleListener;
    private final ExecutionResolver resolver;
    private final RunConfigurationDiscoveryService discoveryService;
    private final RunConfigurationSelectionStore selectionStore;
    private volatile List<RunConfiguration> configurations = List.of();
    private volatile RunConfiguration selectedConfiguration;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private volatile RunSession activeSession;
    private volatile RunRequest lastRequest;
    private volatile LessonRunRequest lastLessonRequest;
    private volatile boolean rerunAfterStop;
    private volatile boolean disposed;
    private final List<RunOutputChunk> outputHistory = new CopyOnWriteArrayList<>();
    private final ExecutorService preparationExecutor = Executors.newSingleThreadExecutor(
            task -> daemon("eyecode-run-preparation", task));
    private final AtomicBoolean completionPublished = new AtomicBoolean(true);
    private volatile boolean hasCompletion;
    private volatile int lastExitCode;
    private volatile boolean lastStopped;
    private volatile RunPhase phase = RunPhase.IDLE;
    private volatile Future<?> preparationTask;
    private volatile boolean preparationCancelled;
    private volatile long attemptGeneration;
    private volatile BooleanSupplier beforeRunFlush = () -> true;

    public RunService(ProjectLifecycleService lifecycleService) {
        this(lifecycleService, (ExecutionResolver) null,
                new RunConfigurationDiscoveryService(), new RunConfigurationSelectionStore());
    }

    public RunService(ProjectLifecycleService lifecycleService, ProjectExecutionResolver resolver) {
        this(lifecycleService, adapt(resolver),
                new RunConfigurationDiscoveryService(), new RunConfigurationSelectionStore());
    }

    public RunService(ProjectLifecycleService lifecycleService, ProjectExecutionResolver resolver,
                      RunConfigurationDiscoveryService discoveryService,
                      RunConfigurationSelectionStore selectionStore) {
        this(lifecycleService, adapt(resolver), discoveryService, selectionStore);
    }

    RunService(ProjectLifecycleService lifecycleService, ExecutionResolver resolver) {
        this(lifecycleService, resolver,
                new RunConfigurationDiscoveryService(), new RunConfigurationSelectionStore());
    }

    private RunService(ProjectLifecycleService lifecycleService, ExecutionResolver resolver,
                       RunConfigurationDiscoveryService discoveryService,
                       RunConfigurationSelectionStore selectionStore) {
        this.lifecycleService = lifecycleService;
        this.resolver = resolver == null ? adapt(new ProjectExecutionResolver()) : resolver;
        this.discoveryService = discoveryService == null ? new RunConfigurationDiscoveryService() : discoveryService;
        this.selectionStore = selectionStore == null ? new RunConfigurationSelectionStore() : selectionStore;
        this.lifecycleListener = this::onProjectChanged;
        if (lifecycleService != null) {
            lifecycleService.addListener(lifecycleListener);
            refreshConfigurations();
        }
    }

    public synchronized boolean runCurrent() {
        ProjectModel project = lifecycleService == null ? null : lifecycleService.currentProject();
        if (project == null) {
            publishOutput("No project is open.", true);
            return false;
        }
        refreshConfigurations();
        if (selectedConfiguration == null) {
            publishOutput("No Run Configuration", true);
            return false;
        }
        return run(new RunRequest(project, selectedConfiguration));
    }

    public synchronized boolean run(RunRequest request) {
        if (disposed || request == null || isRunning()) {
            return false;
        }
        if (!isCurrentProject(request.project())) {
            publishOutput("The selected run configuration belongs to a different project.", true);
            return false;
        }
        if (!beforeRunFlush.getAsBoolean()) {
            publishOutput("Could not save pending editor changes.", true);
            return false;
        }
        lastRequest = request;
        lastLessonRequest = null;
        clearOutput();
        preparationCancelled = false;
        completionPublished.set(false);
        long attempt = ++attemptGeneration;
        publishPhase(RunPhase.PREPARING);
        for (Listener listener : listeners) {
            listener.onStarted(request);
        }
        try {
            preparationTask = preparationExecutor.submit(() -> prepareAndStart(request, attempt));
        } catch (RejectedExecutionException exception) {
            publishOutput("Run preparation is unavailable", true);
            publishFinished(-1, false);
            return false;
        }
        return true;
    }

    public synchronized boolean runLesson(LessonRunRequest request) {
        if (disposed || request == null || isRunning()) return false;
        lastLessonRequest = request;
        lastRequest = null;
        clearOutput();
        preparationCancelled = false;
        completionPublished.set(false);
        long attempt = ++attemptGeneration;
        publishPhase(RunPhase.PREPARING);
        try {
            preparationTask = preparationExecutor.submit(() -> prepareAndStartLesson(request, attempt));
        } catch (RejectedExecutionException exception) {
            publishOutput("Run preparation is unavailable", true);
            publishFinished(-1, false);
            return false;
        }
        return true;
    }

    public void setBeforeRunFlush(BooleanSupplier beforeRunFlush) {
        this.beforeRunFlush = beforeRunFlush == null ? () -> true : beforeRunFlush;
    }

    public synchronized boolean rerun() {
        if (lastLessonRequest != null) return runLesson(lastLessonRequest);
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
        if (!isRunning()) {
            return;
        }
        preparationCancelled = true;
        RunSession session = activeSession;
        if (session != null) {
            session.stop();
            return;
        }
        Future<?> preparation = preparationTask;
        if (preparation != null) {
            preparation.cancel(true);
        }
    }

    public synchronized void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        rerunAfterStop = false;
        preparationCancelled = true;
        attemptGeneration++;
        Future<?> preparation = preparationTask;
        if (preparation != null) {
            preparation.cancel(true);
        }
        if (lifecycleService != null) {
            lifecycleService.removeListener(lifecycleListener);
        }
        RunSession session = activeSession;
        if (session != null) {
            session.dispose();
        }
        activeSession = null;
        preparationExecutor.shutdownNow();
    }

    public boolean isRunning() {
        return phase != RunPhase.IDLE;
    }

    public RunPhase phase() {
        return phase;
    }

    public boolean hasLastRequest() {
        return lastRequest != null || lastLessonRequest != null;
    }

    public boolean hasCompletion() {
        return hasCompletion;
    }

    public Integer lastExitCode() {
        return hasCompletion ? lastExitCode : null;
    }

    public boolean lastStopped() {
        return hasCompletion && lastStopped;
    }

    public List<RunConfiguration> configurations() {
        return configurations;
    }

    public RunConfiguration selectedConfiguration() {
        return selectedConfiguration;
    }

    public synchronized void refreshConfigurations() {
        ProjectModel project = lifecycleService == null ? null : lifecycleService.currentProject();
        if (project == null) {
            configurations = List.of();
            selectedConfiguration = null;
            return;
        }
        List<RunConfiguration> discovered = discoveryService.discover(project);
        configurations = discovered;
        String stored = selectionStore.selectedId(project.getRootDir());
        selectedConfiguration = discovered.stream().filter(value -> value.id().equals(stored)).findFirst()
                .orElseGet(() -> discoveryService.defaultConfiguration(discovered).orElse(null));
    }

    public synchronized boolean selectConfiguration(String id) {
        RunConfiguration next = configurations.stream().filter(value -> value.id().equals(id)).findFirst().orElse(null);
        if (next == null) {
            return false;
        }
        selectedConfiguration = next;
        if (lifecycleService != null && lifecycleService.currentProject() != null) {
            selectionStore.select(lifecycleService.currentProject().getRootDir(), next.id());
        }
        return true;
    }

    public void addListener(Listener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
            if (isRunning() && lastRequest != null) {
                listener.onStarted(lastRequest);
                listener.onPhase(phase);
            }
            for (RunOutputChunk chunk : outputHistory) {
                listener.onOutput(chunk.text(), chunk.error());
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

    private void publishOutput(String text, boolean error) {
        if (text != null) {
            outputHistory.add(new RunOutputChunk(text, error));
        }
        for (Listener listener : listeners) {
            listener.onOutput(text, error);
        }
    }

    private void publishFinished(int exitCode, boolean stopped) {
        if (!completionPublished.compareAndSet(false, true)) {
            return;
        }
        publishPhase(RunPhase.IDLE);
        lastExitCode = exitCode;
        lastStopped = stopped;
        hasCompletion = true;
        for (Listener listener : listeners) {
            listener.onFinished(exitCode, stopped);
        }
        if (rerunAfterStop && !disposed) {
            rerunAfterStop = false;
            RunRequest request = lastRequest;
            if (request != null) {
                run(request);
            }
        }
    }

    private void publishPhase(RunPhase next) {
        phase = next;
        for (Listener listener : listeners) {
            listener.onPhase(next);
        }
    }

    private void prepareAndStart(RunRequest request, long attempt) {
        ResolvedExecution execution;
        try {
            execution = resolver.resolve(request.project(), request.configuration());
        } catch (RuntimeException exception) {
            if (attempt != attemptGeneration || disposed) {
                return;
            }
            if (preparationCancelled) {
                publishFinished(-1, true);
            } else {
                publishOutput(exception.getMessage() == null ? exception.toString() : exception.getMessage(), true);
                publishFinished(-1, false);
            }
            return;
        }

        RunSession session;
        synchronized (this) {
            if (attempt != attemptGeneration || disposed) {
                return;
            }
            if (preparationCancelled || completionPublished.get()) {
                publishFinished(-1, true);
                return;
            }
            session = new RunSession(execution, request.project().getRootDir(), new SessionListener());
            activeSession = session;
        }
        session.start();
    }

    private void prepareAndStartLesson(LessonRunRequest request, long attempt) {
        ResolvedExecution execution;
        try {
            execution = new LessonExecutionResolver().resolve(request);
        } catch (RuntimeException exception) {
            if (!preparationCancelled) publishOutput(exception.getMessage() == null ? exception.toString() : exception.getMessage(), true);
            publishFinished(-1, preparationCancelled);
            return;
        }
        synchronized (this) {
            if (attempt != attemptGeneration || disposed || preparationCancelled || completionPublished.get()) {
                execution.cleanup().run();
                if (!completionPublished.get()) publishFinished(-1, true);
                return;
            }
            activeSession = new RunSession(execution, java.nio.file.Path.of(System.getProperty("java.io.tmpdir")), new SessionListener());
        }
        activeSession.start();
    }

    private static ExecutionResolver adapt(ProjectExecutionResolver resolver) {
        if (resolver == null) return null;
        return (project, configuration) -> configuration == null
                ? resolver.resolve(project) : resolver.resolve(project, configuration);
    }

    private static Thread daemon(String name, Runnable task) {
        Thread thread = new Thread(task, name);
        thread.setDaemon(true);
        return thread;
    }

    private synchronized void onProjectChanged(ProjectModel project) {
        rerunAfterStop = false;
        if (lastRequest != null && !sameProject(lastRequest.project(), project)) {
            lastRequest = null;
        }
        refreshConfigurations();
    }

    private boolean isCurrentProject(ProjectModel project) {
        return lifecycleService == null || sameProject(project, lifecycleService.currentProject());
    }

    private boolean sameProject(ProjectModel first, ProjectModel second) {
        if (first == null || second == null) {
            return first == second;
        }
        Path firstRoot = first.getRootDir().toAbsolutePath().normalize();
        Path secondRoot = second.getRootDir().toAbsolutePath().normalize();
        return firstRoot.equals(secondRoot);
    }
    private final class SessionListener implements RunSession.Listener {
        @Override
        public void onPhase(RunPhase next) {
            publishPhase(next);
        }

        @Override
        public void onOutput(String text, boolean error) {
            publishOutput(text, error);
        }

        @Override
        public synchronized void onFinished(int exitCode, boolean stopped) {
            activeSession = null;
            publishFinished(exitCode, stopped);
        }
    }
}
