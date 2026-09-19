package com.eyecode.ui.web;

import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.runtime.LessonRunRequest;
import com.eyecode.runtime.RunConfiguration;
import com.eyecode.runtime.RunService;
import com.eyecode.terminal.TerminalService;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private final ProjectLifecycleService.Listener terminalWorkspaceListener;
    private final TerminalService.Listener terminalListener;
    private final RunService.Listener runListener;
    private boolean closed;

    WebShellExecutionController(WebShellSurface surface, ProjectLifecycleService projectLifecycleService,
                                RunService runService, TerminalService terminalService) {
        this.surface = java.util.Objects.requireNonNull(surface, "surface");
        this.projectLifecycleService = java.util.Objects.requireNonNull(projectLifecycleService, "projectLifecycleService");
        this.runService = java.util.Objects.requireNonNull(runService, "runService");
        this.terminalService = java.util.Objects.requireNonNull(terminalService, "terminalService");
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
            @Override public void onPhase(com.eyecode.runtime.RunPhase phase) { sendRunState(); }
            @Override public void onStarted(com.eyecode.runtime.RunRequest request) { sendRunState(); }
            @Override public void onOutput(String text, boolean error) {
                if (text == null) {
                    surface.send(WebShellEnvelope.event("run", "output", Map.of("clear", true)));
                } else {
                    surface.send(WebShellEnvelope.event("run", "output", Map.of(
                            "text", text, "error", error)));
                }
            }
            @Override public void onFinished(int exitCode, boolean stopped) { sendRunState(); }
        };
        runService.addListener(runListener);
        surface.registerHandler("run", "state", this::runState);
        surface.registerHandler("run", "run", this::run);
        surface.registerHandler("run", "rerun", this::rerun);
        surface.registerHandler("run", "stop", this::stop);
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
        boolean started = lessonRun(message) ? runService.runLesson(lessonRunRequest(message)) : runService.runCurrent();
        sendRunState();
        return message.response(Map.of("started", started));
    }

    private WebShellEnvelope rerun(WebShellEnvelope message) {
        boolean started = lessonRun(message) ? runService.runLesson(lessonRunRequest(message)) : runService.rerun();
        sendRunState();
        return message.response(Map.of("started", started));
    }

    private WebShellEnvelope stop(WebShellEnvelope message) {
        runService.stop();
        sendRunState();
        return message.response(Map.of("stopped", true));
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
            return new LessonRunRequest.SourceFile(name, source);
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
    }
}
