package com.eyecode.ui.web;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageId;
import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticEducation;
import com.eyecode.language.diagnostics.DiagnosticsRequest;
import com.eyecode.language.diagnostics.DiagnosticsEducationService;
import com.eyecode.language.diagnostics.DiagnosticsResult;
import com.eyecode.language.diagnostics.DiagnosticsService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class WebShellDiagnosticsController {

    private final WebShellSurface surface;
    private final DiagnosticsService diagnostics;
    private final DiagnosticsEducationService education;
    private final ThreadPoolExecutor executor;
    private final Map<String, String> latestRequestByUri = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile boolean disposed;

    WebShellDiagnosticsController(WebShellSurface surface, DiagnosticsService diagnostics) {
        this(surface, diagnostics, null);
    }

    WebShellDiagnosticsController(WebShellSurface surface, DiagnosticsService diagnostics,
                                  DiagnosticsEducationService education) {
        this.surface = surface;
        this.diagnostics = diagnostics;
        this.education = education;
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
        String uri = text(message.payload(), "uri");
        if (uri.isBlank() || message.requestId().isBlank()) {
            return message.error(new WebShellError("INVALID_DIAGNOSTICS_REQUEST",
                    "Diagnostics require a document URI and request id", true));
        }
        LanguageDocument document = document(uri, text(message.payload(), "displayName"),
                text(message.payload(), "language"));
        DiagnosticsRequest request = new DiagnosticsRequest(document, number(message.payload(), "modelVersion"),
                text(message.payload(), "content"));
        latestRequestByUri.put(uri, message.requestId());
        executor.execute(() -> publish(message.requestId(), request));
        return message.response(Map.of("accepted", true, "requestId", message.requestId()));
    }

    private void publish(String requestId, DiagnosticsRequest request) {
        if (disposed || !requestId.equals(latestRequestByUri.get(request.document().uri()))) return;
        DiagnosticsResult result = diagnostics.analyze(request);
        if (disposed || !requestId.equals(latestRequestByUri.get(request.document().uri()))) return;
        if (result.hasInfrastructureError()) {
            surface.send(WebShellEnvelope.event("diagnostics", "failure", Map.of(
                    "uri", request.document().uri(), "requestId", requestId, "modelVersion", request.version(),
                    "message", result.infrastructureError())));
            return;
        }
        surface.send(WebShellEnvelope.event("diagnostics", "publish", payload(requestId, request, result)));
    }

    private Map<String, Object> payload(String requestId, DiagnosticsRequest request, DiagnosticsResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uri", request.document().uri());
        payload.put("requestId", requestId);
        payload.put("modelVersion", request.version());
        payload.put("diagnostics", result.diagnostics().stream().map(diagnostic -> payload(request, diagnostic)).toList());
        return payload;
    }

    private Map<String, Object> payload(DiagnosticsRequest request, Diagnostic diagnostic) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("severity", diagnostic.severity().name());
        payload.put("code", diagnostic.code());
        payload.put("message", diagnostic.message());
        payload.put("startLine", diagnostic.startLine());
        payload.put("startColumn", diagnostic.startColumn());
        payload.put("endLine", diagnostic.endLine());
        payload.put("endColumn", diagnostic.endColumn());
        if (!diagnostic.category().isBlank()) payload.put("category", diagnostic.category());
        if (education != null) education.explain(request.document(), diagnostic).ifPresent(value -> payload.put("education", educationPayload(value)));
        return payload;
    }

    private static Map<String, Object> educationPayload(DiagnosticEducation value) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", value.title());
        payload.put("summary", value.summary());
        payload.put("explanation", value.explanation());
        payload.put("pattern", value.pattern());
        payload.put("correctedPattern", value.correctedPattern());
        payload.put("tip", value.tip());
        payload.put("relatedContent", value.relatedContent().stream().map(related -> Map.of(
                "id", related.id(), "title", related.title(), "description", related.description())).toList());
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

    private LanguageDocument document(String uri, String displayName, String language) {
        LanguageId declared = LanguageId.parse(language).orElse(null);
        return new LanguageDocument(uri, null, displayName, declared);
    }
}
