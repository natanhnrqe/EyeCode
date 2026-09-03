package com.eyecode.javafx.web;

import com.eyecode.diagnostics.JavaDiagnostic;
import com.eyecode.diagnostics.JavaDiagnosticRequest;
import com.eyecode.diagnostics.JavaDiagnosticsResult;
import com.eyecode.diagnostics.JavaSyntaxDiagnosticAnalyzer;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class WebShellDiagnosticsController {

    private final JavaFxWebShellSurface surface;
    private final JavaSyntaxDiagnosticAnalyzer analyzer;
    private final ThreadPoolExecutor executor;
    private final Map<String, String> latestRequestByUri = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean disposed;

    public WebShellDiagnosticsController(JavaFxWebShellSurface surface) {
        this(surface, new JavaSyntaxDiagnosticAnalyzer());
    }

    WebShellDiagnosticsController(JavaFxWebShellSurface surface, JavaSyntaxDiagnosticAnalyzer analyzer) {
        this.surface = surface;
        this.analyzer = analyzer == null ? new JavaSyntaxDiagnosticAnalyzer() : analyzer;
        this.executor = new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
                runnable -> {
                    Thread thread = new Thread(runnable, "eyecode-java-diagnostics");
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.DiscardOldestPolicy());
        surface.registerHandler("diagnostics", "request", this::request);
    }

    public void invalidate(String uri) {
        if (uri != null && !uri.isBlank()) {
            latestRequestByUri.remove(uri);
        }
    }

    public void clear() {
        latestRequestByUri.clear();
        executor.getQueue().clear();
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        clear();
        executor.shutdownNow();
    }

    private WebShellEnvelope request(WebShellEnvelope message) {
        JavaDiagnosticRequest request = new JavaDiagnosticRequest(text(message.payload(), "uri"), message.requestId(),
                number(message.payload(), "modelVersion"), text(message.payload(), "content"));
        if (request.uri().isBlank() || request.requestId().isBlank()) {
            return message.error(new WebShellError("INVALID_DIAGNOSTICS_REQUEST",
                    "Diagnostics require a document URI and request id", true));
        }
        latestRequestByUri.put(request.uri(), request.requestId());
        executor.execute(() -> publish(request));
        return message.response(Map.of("accepted", true, "requestId", request.requestId()));
    }

    private void publish(JavaDiagnosticRequest request) {
        if (disposed || !request.requestId().equals(latestRequestByUri.get(request.uri()))) return;
        JavaDiagnosticsResult result = analyzer.analyze(request);
        if (disposed || !request.requestId().equals(latestRequestByUri.get(request.uri()))) return;
        if (result.hasInfrastructureError()) {
            surface.send(WebShellEnvelope.event("diagnostics", "failure", Map.of(
                    "uri", request.uri(), "requestId", request.requestId(), "modelVersion", request.modelVersion(),
                    "message", result.infrastructureError())));
            return;
        }
        surface.send(WebShellEnvelope.event("diagnostics", "publish", payload(result)));
    }

    private Map<String, Object> payload(JavaDiagnosticsResult result) {
        JavaDiagnosticRequest request = result.request();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uri", request.uri());
        payload.put("requestId", request.requestId());
        payload.put("modelVersion", request.modelVersion());
        payload.put("diagnostics", result.diagnostics().stream().map(this::payload).toList());
        return payload;
    }

    private Map<String, Object> payload(JavaDiagnostic diagnostic) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("severity", diagnostic.severity().name());
        payload.put("code", diagnostic.code());
        payload.put("message", diagnostic.message());
        payload.put("startLine", diagnostic.startLine());
        payload.put("startColumn", diagnostic.startColumn());
        payload.put("endLine", diagnostic.endLine());
        payload.put("endColumn", diagnostic.endColumn());
        return payload;
    }

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static long number(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.longValue() : 0;
    }
}