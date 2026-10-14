package com.eyecode.ui.web;

import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticEducation;
import com.eyecode.language.diagnostics.DiagnosticExplainer;
import com.eyecode.language.diagnostics.DiagnosticSeverity;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.runtime.LessonCompileAnalyzer;
import com.eyecode.runtime.LessonRunRequest;
import com.eyecode.runtime.LessonTrace;
import com.eyecode.runtime.LessonTraceService;
import com.eyecode.runtime.LessonTraceStatus;
import com.eyecode.runtime.RunConfiguration;
import com.eyecode.runtime.RunService;
import com.eyecode.terminal.TerminalService;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Web adapter for workspace execution and terminal state.
 *
 * <p>It translates the stable {@code run/*} and {@code terminal/*} protocol
 * channels without allowing the WebSocket transport to define run semantics.</p>
 */
final class WebShellExecutionController implements AutoCloseable {
    private final WebShellSurface surface;
    private final ProjectLifecycleService projectLifecycleService;
    private final RunService runService;
    private final TerminalService terminalService;
    private final LessonTraceService traceService;
    private final LessonCompileAnalyzer compileAnalyzer;
    private final Optional<DiagnosticExplainer> explainer;
    private final Map<String, String> lessonUrisByFile = new java.util.concurrent.ConcurrentHashMap<>();
    private final AtomicLong traceSequence = new AtomicLong();
    private volatile boolean lessonReachedExecution;
    private volatile LessonRunRequest lastLessonRequest;
    private final ProjectLifecycleService.Listener terminalWorkspaceListener;
    private final TerminalService.Listener terminalListener;
    private final RunService.Listener runListener;
    private boolean closed;

    WebShellExecutionController(WebShellSurface surface, ProjectLifecycleService projectLifecycleService,
                                RunService runService, TerminalService terminalService) {
        this(surface, projectLifecycleService, runService, terminalService, new LessonTraceService());
    }

    WebShellExecutionController(WebShellSurface surface, ProjectLifecycleService projectLifecycleService,
                                RunService runService, TerminalService terminalService,
                                LessonTraceService traceService) {
        this(surface, projectLifecycleService, runService, terminalService, traceService, null);
    }

    WebShellExecutionController(WebShellSurface surface, ProjectLifecycleService projectLifecycleService,
                                RunService runService, TerminalService terminalService,
                                LessonTraceService traceService, DiagnosticExplainer explainer) {
        this.surface = java.util.Objects.requireNonNull(surface, "surface");
        this.projectLifecycleService = java.util.Objects.requireNonNull(projectLifecycleService, "projectLifecycleService");
        this.runService = java.util.Objects.requireNonNull(runService, "runService");
        this.terminalService = java.util.Objects.requireNonNull(terminalService, "terminalService");
        this.traceService = java.util.Objects.requireNonNull(traceService, "traceService");
        this.compileAnalyzer = new LessonCompileAnalyzer();
        this.explainer = Optional.ofNullable(explainer);
        this.terminalWorkspaceListener = project -> {
            terminalService.setWorkspaceDirectory(project == null ? null : project.getRootDir());
            sendTerminalState();
        };
        projectLifecycleService.addListener(terminalWorkspaceListener);
        terminalListener = new TerminalService.Listener() {
            @Override public void onStarted(Path workingDirectory) { sendTerminalState(); }
            @Override public void onOutput(String text, boolean error) { }
            @Override public void onFinished(int exitCode, boolean stopped) { sendTerminalState(); }
        };
        terminalService.addListener(terminalListener);
        runListener = new RunService.Listener() {
            @Override public void onPhase(com.eyecode.runtime.RunPhase phase) {
                if (phase == com.eyecode.runtime.RunPhase.RUNNING) {
                    lessonReachedExecution = true;
                }
                sendRunState();
            }
            @Override public void onStarted(com.eyecode.runtime.RunRequest request) { sendRunState(); }
            @Override public void onOutput(String text, boolean error) {
                if (text == null) {
                    surface.send(WebShellEnvelope.event("run", "output", Map.of("clear", true)));
                } else {
                    surface.send(WebShellEnvelope.event("run", "output", Map.of(
                            "text", text, "error", error)));
                }
            }
            @Override public void onFinished(int exitCode, boolean stopped) {
                publishLessonRunDiagnostics(exitCode, stopped);
                sendRunState();
            }
        };
        runService.addListener(runListener);
        surface.registerHandler("run", "state", this::runState);
        surface.registerHandler("run", "run", this::run);
        surface.registerHandler("run", "rerun", this::rerun);
        surface.registerHandler("run", "stop", this::stop);
        surface.registerHandler("run", "trace", this::trace);
        surface.registerHandler("run", "traceStop", this::traceStop);
        surface.registerHandler("run", "selectConfiguration", this::selectRunConfiguration);
        surface.registerHandler("terminal", "show", this::showTerminal);
        surface.registerHandler("terminal", "hide", this::hideTerminal);
        surface.registerHandler("terminal", "restart", this::restartTerminal);
        surface.registerHandler("terminal", "resize", this::resizeTerminal);
        surface.registerHandler("terminal", "state", this::terminalState);
        surface.registerHandler("terminal", "status", this::terminalState);
        surface.registerHandler("terminal", "stop", this::stopTerminal);
    }

    void publishWorkspaceState() {
        sendRunState();
    }

    private WebShellEnvelope runState(WebShellEnvelope message) {
        return message.response(runPayload());
    }

    private WebShellEnvelope run(WebShellEnvelope message) {
        boolean lesson = lessonRun(message);
        if (!lesson) {
            lastLessonRequest = null;
        }
        boolean started = lesson
                ? runLesson(message, runService::runLesson)
                : runService.runCurrent();
        sendRunState();
        return message.response(Map.of("started", started));
    }

    private WebShellEnvelope rerun(WebShellEnvelope message) {
        boolean lesson = lessonRun(message);
        if (!lesson) {
            lastLessonRequest = null;
        }
        boolean started = lesson
                ? runLesson(message, runService::runLesson)
                : runService.rerun();
        sendRunState();
        return message.response(Map.of("started", started));
    }

    private boolean runLesson(WebShellEnvelope message, java.util.function.Function<LessonRunRequest, Boolean> starter) {
        LessonRunRequest request = lessonRunRequest(message);
        rememberLessonRequest(request);
        lessonReachedExecution = false;
        return starter.apply(request);
    }

    private void rememberLessonRequest(LessonRunRequest request) {
        lastLessonRequest = request;
        lessonUrisByFile.clear();
        for (LessonRunRequest.SourceFile file : request.files()) {
            if (!file.uri().isBlank()) {
                lessonUrisByFile.put(file.name(), file.uri());
            }
        }
    }

    private WebShellEnvelope stop(WebShellEnvelope message) {
        runService.stop();
        traceService.stop();
        sendRunState();
        return message.response(Map.of("stopped", true));
    }

    private WebShellEnvelope trace(WebShellEnvelope message) {
        long traceId = traceSequence.incrementAndGet();
        LessonRunRequest request = lessonRunRequest(message);
        rememberLessonRequest(request);
        boolean started = traceService.trace(request, trace -> publishTrace(traceId, trace));
        return message.response(Map.of("started", started, "traceId", started ? traceId : 0L));
    }

    private WebShellEnvelope traceStop(WebShellEnvelope message) {
        traceService.stop();
        return message.response(Map.of("stopped", true));
    }

    private void publishLessonRunDiagnostics(int exitCode, boolean stopped) {
        LessonRunRequest request = lastLessonRequest;
        if (closed || request == null || lessonReachedExecution || stopped || exitCode == 0) {
            return;
        }
        List<LessonCompileAnalyzer.Entry> entries = compileAnalyzer.analyze(request);
        if (entries.isEmpty()) {
            return;
        }
        List<Map<String, Object>> diagnostics = entries.stream()
                .filter(entry -> !entry.uri().isBlank())
                .map(entry -> diagnosticPayload(entry.uri(), entry.severity(), entry.code(), entry.message(),
                        entry.startLine(), entry.startColumn(), entry.endLine(), entry.endColumn()))
                .toList();
        if (!diagnostics.isEmpty()) {
            surface.send(WebShellEnvelope.event("diagnostics", "lessonRun", Map.of("diagnostics", diagnostics)));
        }
    }

    private void publishRuntimeErrorDiagnostic(LessonTrace trace) {
        if (closed || trace.status() != LessonTraceStatus.RUNTIME_ERROR
                || trace.runtimeError() == null || trace.runtimeErrorLine() <= 0) {
            return;
        }
        String uri = lessonUrisByFile.get(trace.mainClass() + ".java");
        if (uri == null || uri.isBlank()) {
            return;
        }
        surface.send(WebShellEnvelope.event("diagnostics", "lessonRun", Map.of("diagnostics",
                List.of(diagnosticPayload(uri, "ERROR", "runtime", trace.runtimeError(),
                        trace.runtimeErrorLine(), 1, trace.runtimeErrorLine(), 1)))));
    }

    private Map<String, Object> diagnosticPayload(String uri, String severity, String code, String message,
                                                  int startLine, int startColumn, int endLine, int endColumn) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uri", uri);
        payload.put("severity", severity);
        payload.put("code", code);
        payload.put("message", message);
        payload.put("startLine", startLine);
        payload.put("startColumn", startColumn);
        payload.put("endLine", endLine);
        payload.put("endColumn", endColumn);
        Diagnostic synthetic = new Diagnostic(severity(severity), code, message, startLine, startColumn, endLine, endColumn, "");
        explainer.flatMap(explainer -> explainer.explain(synthetic))
                .ifPresent(education -> payload.put("education", educationPayload(education)));
        return payload;
    }

    private static DiagnosticSeverity severity(String severity) {
        return "WARNING".equals(severity) ? DiagnosticSeverity.WARNING : DiagnosticSeverity.ERROR;
    }

    private static Map<String, Object> educationPayload(DiagnosticEducation education) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", education.title());
        payload.put("summary", education.summary());
        payload.put("explanation", education.explanation());
        payload.put("pattern", education.pattern());
        payload.put("correctedPattern", education.correctedPattern());
        payload.put("tip", education.tip());
        payload.put("relatedContent", education.relatedContent().stream().map(related -> Map.of(
                "id", related.id(), "title", related.title(), "description", related.description())).toList());
        return payload;
    }

    private void publishTrace(long traceId, LessonTrace trace) {
        if (!closed) {
            surface.send(WebShellEnvelope.event("run", "trace", tracePayload(traceId, trace)));
            publishRuntimeErrorDiagnostic(trace);
        }
    }

    private Map<String, Object> tracePayload(long traceId, LessonTrace trace) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("traceId", traceId);
        payload.put("mainClass", trace.mainClass());
        payload.put("status", trace.status().name());
        payload.put("exitCode", trace.exitCode());
        payload.put("runtimeError", trace.runtimeError() == null ? "" : trace.runtimeError());
        payload.put("runtimeErrorLine", trace.runtimeErrorLine());
        payload.put("steps", trace.steps().stream()
                .map(step -> {
                    java.util.LinkedHashMap<String, Object> stepPayload = new java.util.LinkedHashMap<>();
                    stepPayload.put("index", step.index());
                    stepPayload.put("className", step.className());
                    stepPayload.put("line", step.line());
                    stepPayload.put("variables", step.variables().stream()
                            .map(variable -> variablePayload(variable)).toList());
                    return stepPayload;
                }).toList());
        payload.put("output", trace.output().stream()
                .map(chunk -> Map.of("text", chunk.text(), "error", chunk.error())).toList());
        payload.put("limitations", trace.limitations());
        return payload;
    }

    private Map<String, Object> variablePayload(LessonTrace.Variable variable) {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("name", variable.name());
        payload.put("type", variable.type());
        payload.put("value", variable.value());
        payload.put("kind", variable.kind());
        payload.put("children", variable.children().stream()
                .map(child -> variablePayload(child)).toList());
        payload.put("truncated", variable.truncated());
        return payload;
    }

    private WebShellEnvelope selectRunConfiguration(WebShellEnvelope message) {
        boolean selected = runService.selectConfiguration(text(message.payload(), "id"));
        sendRunState();
        return message.response(Map.of("selected", selected));
    }

    private WebShellEnvelope showTerminal(WebShellEnvelope message) {
        TerminalService.Status status = terminalService.show();
        sendTerminalState();
        return message.response(terminalStatusPayload(status));
    }

    private WebShellEnvelope hideTerminal(WebShellEnvelope message) {
        terminalService.hide();
        sendTerminalState();
        return message.response(terminalStatusPayload(terminalService.status()));
    }

    private WebShellEnvelope resizeTerminal(WebShellEnvelope message) {
        terminalService.resize((int) number(message.payload(), "cols", 0),
                (int) number(message.payload(), "rows", 0));
        return message.response(Map.of("updated", true));
    }

    private WebShellEnvelope terminalState(WebShellEnvelope message) {
        return message.response(terminalStatusPayload(terminalService.status()));
    }

    private WebShellEnvelope restartTerminal(WebShellEnvelope message) {
        boolean restarted = terminalService.restart();
        sendTerminalState();
        return message.response(Map.of("restarted", restarted));
    }

    private WebShellEnvelope stopTerminal(WebShellEnvelope message) {
        boolean stopped = terminalService.stop();
        sendTerminalState();
        return message.response(Map.of("stopped", stopped));
    }

    private Map<String, Object> runPayload() {
        java.util.LinkedHashMap<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("running", runService.isRunning());
        payload.put("phase", runService.phase().name());
        payload.put("finished", runService.hasCompletion());
        payload.put("exitCode", runService.lastExitCode());
        payload.put("stopped", runService.lastStopped());
        payload.put("rerunAvailable", runService.hasLastRequest());
        payload.put("configurations", runService.configurations().stream()
                .map(this::runConfigurationPayload).toList());
        RunConfiguration selected = runService.selectedConfiguration();
        payload.put("selectedConfigurationId", selected == null ? "" : selected.id());
        return payload;
    }

    private Map<String, Object> runConfigurationPayload(RunConfiguration configuration) {
        return Map.of("id", configuration.id(), "name", configuration.displayName(),
                "mainClass", configuration.mainClass(), "kind", configuration.kind().name());
    }

    private void sendRunState() {
        if (!closed) {
            surface.send(WebShellEnvelope.event("run", "state", runPayload()));
        }
    }

    private void sendTerminalState() {
        if (!closed) {
            surface.send(WebShellEnvelope.event("terminal", "state", terminalStatusPayload(terminalService.status())));
        }
    }

    private Map<String, Object> terminalStatusPayload(TerminalService.Status status) {
        return Map.of(
                "requested", status.requested(),
                "running", status.running(),
                "workingDirectory", status.workingDirectory(),
                "endpoint", status.endpoint());
    }

    private static boolean lessonRun(WebShellEnvelope message) {
        return "lesson".equals(message.payload().get("context"));
    }

    private static LessonRunRequest lessonRunRequest(WebShellEnvelope message) {
        Object workspace = message.payload().get("lessonWorkspace");
        if (!(workspace instanceof Map<?, ?> values) || !(values.get("files") instanceof List<?> files)) {
            throw new IllegalArgumentException("Lesson workspace is required");
        }
        List<LessonRunRequest.SourceFile> sources = files.stream().map(value -> {
            if (!(value instanceof Map<?, ?> file) || !(file.get("name") instanceof String name)
                    || !(file.get("source") instanceof String source)) {
                throw new IllegalArgumentException("Invalid lesson source file");
            }
            Object uri = file.get("uri");
            return new LessonRunRequest.SourceFile(name, source,
                    uri instanceof String value1 ? value1 : "");
        }).toList();
        if (!(values.get("mainClass") instanceof String mainClass)) {
            throw new IllegalArgumentException("Lesson main class is required");
        }
        return new LessonRunRequest(sources, mainClass);
    }

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long number(Map<String, Object> payload, String key, long fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }
        closed = true;
        projectLifecycleService.removeListener(terminalWorkspaceListener);
        terminalService.removeListener(terminalListener);
        runService.removeListener(runListener);
        traceService.close();
    }
}
