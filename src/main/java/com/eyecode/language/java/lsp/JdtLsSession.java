package com.eyecode.language.java.lsp;

import org.eclipse.lsp4j.ApplyWorkspaceEditParams;
import org.eclipse.lsp4j.ApplyWorkspaceEditResponse;
import org.eclipse.lsp4j.ClientCapabilities;
import org.eclipse.lsp4j.CompletionCapabilities;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.ConfigurationParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.MessageActionItem;
import org.eclipse.lsp4j.MessageParams;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.ShowMessageRequestParams;
import org.eclipse.lsp4j.TextDocumentClientCapabilities;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentItem;
import org.eclipse.lsp4j.VersionedTextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentContentChangeEvent;
import org.eclipse.lsp4j.WorkspaceClientCapabilities;
import org.eclipse.lsp4j.WorkspaceFolder;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification;
import org.eclipse.lsp4j.launch.LSPLauncher;
import org.eclipse.lsp4j.services.LanguageClient;
import org.eclipse.lsp4j.services.LanguageServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class JdtLsSession implements AutoCloseable {
    private static final Duration CLOSE_TIMEOUT = Duration.ofSeconds(15);

    private final JdtLsProcessConfiguration configuration;
    private final JdtLsClient client;
    private final ExecutorService lspExecutor;
    private final ExecutorService stderrExecutor;
    private volatile JdtLsLifecycleState state = JdtLsLifecycleState.NEW;
    private volatile Process process;
    private volatile LanguageServer server;
    private volatile InitializeResult initializeResult;
    private volatile Throwable failure;
    private volatile Future<?> listener;

    public JdtLsSession(JdtLsProcessConfiguration configuration) {
        this.configuration = Objects.requireNonNull(configuration, "configuration");
        this.client = new JdtLsClient(configuration.workspaceDirectory());
        lspExecutor = Executors.newCachedThreadPool(namedDaemonFactory("eyecode-jdtls-lsp"));
        stderrExecutor = Executors.newSingleThreadExecutor(namedDaemonFactory("eyecode-jdtls-stderr"));
    }

    public synchronized void start() {
        requireState(JdtLsLifecycleState.NEW, "START");
        state = JdtLsLifecycleState.STARTING;
        event("starting");
        try {
            Files.createDirectories(configuration.dataDirectory());
            if (sameOrNested(configuration.dataDirectory(), configuration.workspaceDirectory())
                    || sameOrNested(configuration.workspaceDirectory(), configuration.dataDirectory())) {
                throw new IllegalArgumentException("JDT LS data directory must be isolated from its workspace directory");
            }
            process = new ProcessBuilder(configuration.command())
                    .directory(configuration.installationRoot().toFile())
                    .redirectErrorStream(false)
                    .start();
            stderrExecutor.execute(() -> captureStderr(process));
            Launcher<LanguageServer> launcher = LSPLauncher.createClientLauncher(client, process.getInputStream(),
                    process.getOutputStream(), lspExecutor, consumer -> consumer);
            server = launcher.getRemoteProxy();
            listener = launcher.startListening();
            process.onExit().thenAccept(ignored -> onProcessExit());
        } catch (IOException | RuntimeException exception) {
            failure = exception;
            state = JdtLsLifecycleState.FAILED;
            close();
            throw new IllegalStateException("JDT LS START failed: " + exception.getMessage(), exception);
        }
    }

    public synchronized InitializeResult initialize(Duration timeout) {
        requireState(JdtLsLifecycleState.STARTING, "INITIALIZE");
        state = JdtLsLifecycleState.INITIALIZING;
        event("initializing");
        InitializeParams params = new InitializeParams();
        params.setProcessId(Math.toIntExact(ProcessHandle.current().pid()));
        params.setRootUri(configuration.workspaceDirectory().toUri().toString());
        params.setWorkspaceFolders(List.of(new WorkspaceFolder(configuration.workspaceDirectory().toUri().toString(), "eyecode-jdtls-spike")));
        params.setCapabilities(clientCapabilities());
        params.setInitializationOptions(configuration.initializationOptions());
        try {
            initializeResult = await(server.initialize(params), timeout, "INITIALIZE");
            server.initialized(new InitializedParams());
            state = JdtLsLifecycleState.READY;
            event("ready");
            return initializeResult;
        } catch (RuntimeException exception) {
            failure = exception;
            state = JdtLsLifecycleState.FAILED;
            throw exception;
        }
    }

    public void didOpen(String uri, String source, int version) {
        requireState(JdtLsLifecycleState.READY, "DID_OPEN");
        TextDocumentItem document = new TextDocumentItem(uri, "java", version, source == null ? "" : source);
        server.getTextDocumentService().didOpen(new DidOpenTextDocumentParams(document));
        event("didOpen");
    }

    public void didChange(String uri, String source, int version) {
        requireState(JdtLsLifecycleState.READY, "DID_CHANGE");
        VersionedTextDocumentIdentifier document = new VersionedTextDocumentIdentifier(uri, version);
        server.getTextDocumentService().didChange(new DidChangeTextDocumentParams(document,
                List.of(new TextDocumentContentChangeEvent(source == null ? "" : source))));
        event("didChange");
    }

    public void didSave(String uri, String source) {
        requireState(JdtLsLifecycleState.READY, "DID_SAVE");
        server.getTextDocumentService().didSave(new DidSaveTextDocumentParams(new TextDocumentIdentifier(uri), source));
        event("didSave");
    }

    public void didClose(String uri) {
        if (state != JdtLsLifecycleState.READY) return;
        server.getTextDocumentService().didClose(new DidCloseTextDocumentParams(new TextDocumentIdentifier(uri)));
        event("didClose");
    }

    public List<CompletionItem> completion(String uri, int line, int character, Duration timeout) {
        requireState(JdtLsLifecycleState.READY, "COMPLETION");
        CompletionParams params = new CompletionParams();
        params.setTextDocument(new TextDocumentIdentifier(uri));
        params.setPosition(new org.eclipse.lsp4j.Position(line, character));
        event("completion request");
        Either<List<CompletionItem>, CompletionList> result = await(server.getTextDocumentService().completion(params), timeout, "COMPLETION");
        return result.isLeft() ? List.copyOf(result.getLeft()) : List.copyOf(result.getRight().getItems());
    }

    public JdtLsLifecycleState state() {
        return state;
    }

    public InitializeResult initializeResult() {
        return initializeResult;
    }

    public Throwable failure() {
        return failure;
    }

    public boolean isProcessAlive() {
        Process current = process;
        return current != null && current.isAlive();
    }

    public List<String> stderrLines() {
        return client.stderrLines();
    }

    @Override
    public synchronized void close() {
        if (state == JdtLsLifecycleState.STOPPED) return;
        if (state == JdtLsLifecycleState.NEW) {
            state = JdtLsLifecycleState.STOPPED;
            shutdownExecutors();
            return;
        }
        state = JdtLsLifecycleState.SHUTTING_DOWN;
        event("shutting down");
        try {
            if (server != null && initializeResult != null) {
                await(server.shutdown(), CLOSE_TIMEOUT, "SHUTDOWN");
                server.exit();
            }
        } catch (RuntimeException exception) {
            failure = failure == null ? exception : failure;
        } finally {
            stopProcess();
            shutdownExecutors();
            state = isProcessAlive() ? JdtLsLifecycleState.FAILED : JdtLsLifecycleState.STOPPED;
            event(state == JdtLsLifecycleState.STOPPED ? "stopped" : "failed to stop");
        }
    }

    private static ClientCapabilities clientCapabilities() {
        ClientCapabilities capabilities = new ClientCapabilities();
        WorkspaceClientCapabilities workspace = new WorkspaceClientCapabilities();
        workspace.setConfiguration(true);
        workspace.setWorkspaceFolders(true);
        capabilities.setWorkspace(workspace);
        TextDocumentClientCapabilities textDocument = new TextDocumentClientCapabilities();
        textDocument.setCompletion(new CompletionCapabilities());
        capabilities.setTextDocument(textDocument);
        return capabilities;
    }

    private <T> T await(CompletableFuture<T> operation, Duration timeout, String stage) {
        Duration bounded = timeout == null || timeout.isNegative() || timeout.isZero() ? CLOSE_TIMEOUT : timeout;
        try {
            return operation.get(bounded.toMillis(), TimeUnit.MILLISECONDS);
        } catch (java.util.concurrent.TimeoutException exception) {
            throw new IllegalStateException("JDT LS " + stage + " timed out after " + bounded, exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("JDT LS " + stage + " was interrupted", exception);
        } catch (java.util.concurrent.ExecutionException exception) {
            throw new IllegalStateException("JDT LS " + stage + " failed: " + message(exception.getCause()), exception.getCause());
        }
    }

    private void captureStderr(Process current) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(current.getErrorStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) client.recordStderr(line);
        } catch (IOException ignored) {
        }
    }

    private synchronized void onProcessExit() {
        if (state == JdtLsLifecycleState.STOPPED || state == JdtLsLifecycleState.SHUTTING_DOWN) return;
        failure = new IllegalStateException("JDT LS process exited unexpectedly");
        state = JdtLsLifecycleState.FAILED;
        shutdownExecutors();
    }

    private void stopProcess() {
        Process current = process;
        if (current == null) return;
        try {
            current.getOutputStream().close();
        } catch (IOException ignored) {
        }
        if (waitFor(current, CLOSE_TIMEOUT)) return;
        destroyTree(current, false);
        if (waitFor(current, Duration.ofSeconds(3))) return;
        destroyTree(current, true);
        waitFor(current, Duration.ofSeconds(3));
    }

    private void shutdownExecutors() {
        if (listener != null) listener.cancel(true);
        lspExecutor.shutdownNow();
        stderrExecutor.shutdownNow();
        awaitTermination(lspExecutor);
        awaitTermination(stderrExecutor);
    }

    private static boolean waitFor(Process process, Duration timeout) {
        try {
            return process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private static void destroyTree(Process process, boolean forcibly) {
        process.toHandle().descendants().forEach(handle -> {
            if (forcibly) handle.destroyForcibly(); else handle.destroy();
        });
        if (forcibly) process.destroyForcibly(); else process.destroy();
    }

    private static void awaitTermination(ExecutorService executor) {
        try {
            executor.awaitTermination(2, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private static boolean sameOrNested(java.nio.file.Path first, java.nio.file.Path second) {
        java.nio.file.Path normalizedFirst = first.toAbsolutePath().normalize();
        java.nio.file.Path normalizedSecond = second.toAbsolutePath().normalize();
        return normalizedFirst.startsWith(normalizedSecond);
    }

    private void requireState(JdtLsLifecycleState expected, String stage) {
        if (state != expected) {
            throw new IllegalStateException("JDT LS " + stage + " requires " + expected + " but was " + state);
        }
    }

    private void event(String value) {
        System.getLogger(JdtLsSession.class.getName()).log(System.Logger.Level.INFO, "[JDT-LS] " + value);
    }

    private static String message(Throwable throwable) {
        return throwable == null || throwable.getMessage() == null ? throwable == null ? "unknown failure" : throwable.getClass().getSimpleName() : throwable.getMessage();
    }

    private static java.util.concurrent.ThreadFactory namedDaemonFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return runnable -> {
            Thread thread = new Thread(runnable, prefix + "-" + sequence.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        };
    }

    private static final class JdtLsClient implements LanguageClient, JdtLsStatusClient {
        private static final int STDERR_LIMIT = 80;
        private final java.nio.file.Path workspace;
        private final Deque<String> stderr = new ArrayDeque<>();

        private JdtLsClient(java.nio.file.Path workspace) {
            this.workspace = workspace;
        }

        @Override
        public void telemetryEvent(Object object) {
        }

        @Override
        public void publishDiagnostics(PublishDiagnosticsParams diagnostics) {
        }

        @Override
        public void showMessage(MessageParams message) {
            recordStderr("server message: " + message.getMessage());
        }

        @Override
        public CompletableFuture<MessageActionItem> showMessageRequest(ShowMessageRequestParams request) {
            recordStderr("server message request: " + request.getMessage());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public void logMessage(MessageParams message) {
            recordStderr("server log: " + message.getMessage());
        }

        @Override
        public void status(Map<String, Object> report) {
            Object type = report == null ? null : report.get("type");
            recordStderr("server status: " + (type == null ? "unknown" : type));
        }

        @Override
        public CompletableFuture<ApplyWorkspaceEditResponse> applyEdit(ApplyWorkspaceEditParams params) {
            ApplyWorkspaceEditResponse response = new ApplyWorkspaceEditResponse();
            response.setApplied(false);
            response.setFailureReason("JDT LS spike does not apply workspace edits");
            return CompletableFuture.completedFuture(response);
        }

        @Override
        public CompletableFuture<List<org.eclipse.lsp4j.WorkspaceFolder>> workspaceFolders() {
            return CompletableFuture.completedFuture(List.of(new WorkspaceFolder(workspace.toUri().toString(), "eyecode-jdtls-spike")));
        }

        @Override
        public CompletableFuture<List<Object>> configuration(ConfigurationParams params) {
            int size = params.getItems() == null ? 0 : params.getItems().size();
            return CompletableFuture.completedFuture(java.util.Collections.nCopies(size, Map.of()));
        }

        private synchronized void recordStderr(String line) {
            if (line == null || line.isBlank()) return;
            if (stderr.size() == STDERR_LIMIT) stderr.removeFirst();
            stderr.addLast(line.length() > 500 ? line.substring(0, 500) : line);
        }

        private synchronized List<String> stderrLines() {
            return List.copyOf(new ArrayList<>(stderr));
        }
    }

    private interface JdtLsStatusClient {
        @JsonNotification("language/status")
        void status(Map<String, Object> report);
    }
}
