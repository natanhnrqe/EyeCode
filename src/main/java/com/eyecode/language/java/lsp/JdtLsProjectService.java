package com.eyecode.language.java.lsp;

import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.hover.HoverResult;
import com.eyecode.language.navigation.JdtLsNavigationService;
import com.eyecode.language.navigation.NavigationService;
import com.eyecode.language.refactor.PrepareRenameResult;
import com.eyecode.language.refactor.RenamePlan;
import com.eyecode.language.signature.SignatureHelpResult;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.model.ProjectModel;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class JdtLsProjectService implements AutoCloseable, ProjectLifecycleService.Listener {
    private static final Duration INITIALIZE_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration COMPLETION_TIMEOUT = Duration.ofMillis(900);
    private static final Duration FEATURE_TIMEOUT = Duration.ofSeconds(5);
    private final ProjectLifecycleService projects;
    private final JdtLsDocumentSync documentSync = new JdtLsDocumentSync();
    private final ExecutorService startup = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "eyecode-jdtls-project");
        thread.setDaemon(true);
        return thread;
    });
    private volatile JdtLsSession session;
    private volatile JdtLsProjectCompletion completion;
    private volatile NavigationService navigation;
    private volatile JdtLsProjectDiagnostics diagnostics;
    private volatile JdtLsProjectDiagnostics.Listener jdtDiagnosticsListener;

    public JdtLsProjectService(ProjectLifecycleService projects) {
        this.projects = projects;
        projects.addListener(this);
        if (projects.currentProject() != null) onProjectChanged(projects.currentProject());
    }

    @Override
    public synchronized void onProjectChanged(ProjectModel project) {
        closeSession();
        if (project == null) return;
        String home = System.getProperty("eyecode.jdtls.home", "").trim();
        if (home.isEmpty()) return;
        Path workspace = project.getRootDir().toAbsolutePath().normalize();
        startup.execute(() -> start(Path.of(home), workspace));
    }

    public Optional<CompletionResult> complete(CompletionRequest request) {
        JdtLsProjectCompletion current = completion;
        if (current == null || !eligible(request)) return Optional.empty();
        return current.complete(request, COMPLETION_TIMEOUT);
    }

    public Optional<HoverResult> hover(LanguageFeatureRequest request) {
        JdtLsProjectCompletion current = completion;
        if (current == null || !eligible(request.document().sourceFile(), request.document().uri())) return Optional.empty();
        return current.hover(request, FEATURE_TIMEOUT);
    }

    public Optional<SignatureHelpResult> signatureHelp(LanguageFeatureRequest request) {
        JdtLsProjectCompletion current = completion;
        if (current == null || !eligible(request.document().sourceFile(), request.document().uri())) return Optional.empty();
        return current.signatureHelp(request, FEATURE_TIMEOUT);
    }

    public Optional<NavigationService> navigation() {
        return Optional.ofNullable(navigation);
    }

    public JdtLsProjectDiagnostics diagnostics() {
        return diagnostics;
    }

    public void onJdtDiagnostics(JdtLsProjectDiagnostics.Listener listener) {
        this.jdtDiagnosticsListener = listener;
        JdtLsProjectDiagnostics current = diagnostics;
        if (current != null) current.setListener(listener);
    }

    public Optional<JdtLsCompletionResolveResult> resolveCompletion(com.eyecode.language.completion.CompletionCandidate candidate) {
        JdtLsProjectCompletion current = completion;
        if (current == null || candidate == null || candidate.resolveId().isEmpty()) return Optional.empty();
        try {
            return Optional.ofNullable(current.resolveCandidate(candidate, FEATURE_TIMEOUT));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public synchronized void closeDocument(Path file) {
        JdtLsProjectCompletion current = completion;
        if (current != null) current.close(file);
    }

    public JdtLsLifecycleState state() {
        JdtLsSession current = session;
        return current == null ? JdtLsLifecycleState.STOPPED : current.state();
    }

    private void start(Path installation, Path workspace) {
        try {
            Path data = Path.of(System.getProperty("java.io.tmpdir"), "eyecode-jdtls", identity(workspace));
            JdtLsSession created = new JdtLsSession(JdtLsProcessConfiguration.fromInstallation(
                    JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)), installation, data, workspace));
            synchronized (this) { session = created; }
            created.start();
            created.initialize(INITIALIZE_TIMEOUT);
            synchronized (this) {
                if (session == created && created.state() == JdtLsLifecycleState.READY) {
                    completion = new JdtLsProjectCompletion(created, documentSync);
                    diagnostics = new JdtLsProjectDiagnostics(documentSync);
                    onSessionReady(created, documentSync);
                }
                else created.close();
            }
        } catch (RuntimeException exception) {
            JdtLsSession current = session;
            if (current != null) current.close();
        }
    }

    private boolean eligible(CompletionRequest request) {
        return eligible(request.document().sourceFile(), request.document().uri());
    }

    private boolean eligible(Path file, String uri) {
        if (uri.startsWith("lesson://") || file == null) return false;
        ProjectModel project = projects.currentProject();
        return project != null && file.toAbsolutePath().normalize().startsWith(project.getRootDir());
    }

    private synchronized void closeSession() {
        JdtLsProjectCompletion previous = completion;
        completion = null;
        if (previous != null) previous.clearState();
        documentSync.reset();
        onSessionClosed();
        diagnostics = null;
        JdtLsSession current = session;
        session = null;
        if (current != null) current.close();
    }

    JdtLsDocumentSync documentSync() {
        return documentSync;
    }

    void onSessionReady(JdtLsSession session, JdtLsDocumentSync documents) {
        navigation = new JdtLsNavigationService(session, documentSync());
        JdtLsProjectDiagnostics current = diagnostics;
        if (current != null && session != null) {
            current.attach(session);
            current.setListener(jdtDiagnosticsListener);
        }
    }

    void onSessionClosed() {
        navigation = null;
        JdtLsProjectDiagnostics current = diagnostics;
        if (current != null) current.detach();
    }

    private static String identity(Path workspace) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(workspace.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash, 0, 12);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    public Optional<PrepareRenameResult> prepareRename(Path file, String source, long version, int caretOffset) {
        JdtLsSession current = session;
        if (current == null || !eligible(file, uriOf(file))) return Optional.empty();
        synchronized (current) {
            if (current.state() != JdtLsLifecycleState.READY) return Optional.empty();
            return new JdtLsProjectRefactor(current, projectRoot())
                    .prepareRename(file, source, version, caretOffset, FEATURE_TIMEOUT);
        }
    }

    public Optional<RenamePlan> rename(Path file, String source, long version, int caretOffset, String newName) {
        JdtLsSession current = session;
        if (current == null || !eligible(file, uriOf(file))) return Optional.empty();
        synchronized (current) {
            if (current.state() != JdtLsLifecycleState.READY) return Optional.empty();
            return new JdtLsProjectRefactor(current, projectRoot())
                    .rename(file, source, version, caretOffset, newName, FEATURE_TIMEOUT);
        }
    }

    private Path projectRoot() {
        var project = projects.currentProject();
        return project == null ? null : project.getRootDir();
    }

    private static String uriOf(Path file) {
        return file == null ? "" : file.toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public synchronized void close() {
        projects.removeListener(this);
        closeSession();
        startup.shutdownNow();
    }
}
