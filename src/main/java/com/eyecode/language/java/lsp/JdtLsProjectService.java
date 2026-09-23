package com.eyecode.language.java.lsp;

import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.hover.HoverResult;
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
    private final ExecutorService startup = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "eyecode-jdtls-project");
        thread.setDaemon(true);
        return thread;
    });
    private volatile JdtLsSession session;
    private volatile JdtLsProjectCompletion completion;

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
                if (session == created && created.state() == JdtLsLifecycleState.READY) completion = new JdtLsProjectCompletion(created);
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
        completion = null;
        JdtLsSession current = session;
        session = null;
        if (current != null) current.close();
    }

    private static String identity(Path workspace) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(workspace.toString().getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(hash, 0, 12);
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public synchronized void close() {
        projects.removeListener(this);
        closeSession();
        startup.shutdownNow();
    }
}
