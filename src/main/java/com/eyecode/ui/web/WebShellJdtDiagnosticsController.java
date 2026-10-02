package com.eyecode.ui.web;

import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.QuickFix;
import com.eyecode.language.diagnostics.QuickFixEdit;
import com.eyecode.language.java.lsp.JdtLsProjectDiagnostics;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.ui.web.monaco.MonacoModelId;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public final class WebShellJdtDiagnosticsController {

    private final WebShellSurface surface;
    private final JdtLsProjectService jdt;
    private final ThreadPoolExecutor quickFixExecutor;
    private final ThreadPoolExecutor syncExecutor;
    private volatile boolean disposed;

    WebShellJdtDiagnosticsController(WebShellSurface surface, JdtLsProjectService jdt) {
        this.surface = surface;
        this.jdt = jdt;
        this.quickFixExecutor = singleThreadExecutor("eyecode-jdt-diagnostics");
        this.syncExecutor = singleThreadExecutor("eyecode-jdt-sync");
        surface.registerHandler("diagnostics", "quickFix", this::quickFix);
        surface.registerHandler("diagnostics", "sync", this::sync);
        jdt.onJdtDiagnostics(this::publishJdt);
    }

    private static ThreadPoolExecutor singleThreadExecutor(String threadName) {
        return new ThreadPoolExecutor(1, 1, 30, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1),
                runnable -> {
                    Thread thread = new Thread(runnable, threadName);
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.DiscardOldestPolicy());
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        quickFixExecutor.getQueue().clear();
        quickFixExecutor.shutdownNow();
        syncExecutor.getQueue().clear();
        syncExecutor.shutdownNow();
    }

    private WebShellEnvelope quickFix(WebShellEnvelope message) {
        String uri = text(message.payload(), "uri");
        if (uri.isBlank() || message.requestId().isBlank()) {
            return message.error(new WebShellError("INVALID_QUICKFIX_REQUEST",
                    "Quick fixes require a document URI and request id", true));
        }
        quickFixExecutor.execute(() -> respond(message));
        return message.response(Map.of("accepted", true, "requestId", message.requestId()));
    }

    private WebShellEnvelope sync(WebShellEnvelope message) {
        String uri = text(message.payload(), "uri");
        String content = text(message.payload(), "content");
        if (uri.isBlank() || content.isEmpty()) {
            return message.error(new WebShellError("INVALID_SYNC_REQUEST",
                    "JDT sync requires a document URI and content", true));
        }
        Optional<Path> file = MonacoModelId.pathForModel(uri);
        if (file.isEmpty()) {
            return message.error(new WebShellError("INVALID_SYNC_REQUEST",
                    "JDT sync requires a file URI inside the workspace", true));
        }
        long version = numberLong(message.payload(), "version", 0);
        Path target = file.get();
        syncExecutor.execute(() -> {
            if (disposed) return;
            try {
                jdt.synchronizeDocument(target, content, version);
            } catch (RuntimeException ignored) {
            }
        });
        return message.response(Map.of("accepted", true, "requestId", message.requestId()));
    }

    private void respond(WebShellEnvelope message) {
        if (disposed) return;
        List<QuickFix> fixes;
        try {
            String uri = text(message.payload(), "uri");
            JdtLsProjectDiagnostics diagnostics = jdt.diagnostics();
            Map<String, Object> range = range(message.payload());
            fixes = diagnostics == null ? List.of() : diagnostics.quickFixes(uri,
                    line(range, "startLine"), line(range, "startColumn"),
                    line(range, "endLine"), line(range, "endColumn"));
        } catch (RuntimeException exception) {
            fixes = List.of();
        }
        if (disposed) return;
        surface.send(message.response(Map.of("fixes", fixes.stream().map(WebShellJdtDiagnosticsController::payload).toList())));
    }

    void publishJdt(String uri, List<Diagnostic> diagnostics) {
        if (disposed || uri == null || uri.isBlank()) return;
        surface.send(WebShellEnvelope.event("diagnostics", "jdtPublish", Map.of(
                "uri", uri,
                "diagnostics", diagnostics.stream().map(WebShellJdtDiagnosticsController::payload).toList())));
    }

    private static Map<String, Object> range(Map<String, Object> payload) {
        Object value = payload == null ? null : payload.get("range");
        if (!(value instanceof Map<?, ?> map)) return Map.of();
        Map<String, Object> converted = new LinkedHashMap<>();
        map.forEach((key, entry) -> converted.put(String.valueOf(key), entry));
        return converted;
    }

    private static Map<String, Object> payload(QuickFix fix) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("title", fix.title());
        payload.put("kind", fix.kind());
        payload.put("edits", fix.edits().stream().map(WebShellJdtDiagnosticsController::payload).toList());
        return payload;
    }

    private static Map<String, Object> payload(QuickFixEdit edit) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("startLine", edit.startLine());
        payload.put("startColumn", edit.startColumn());
        payload.put("endLine", edit.endLine());
        payload.put("endColumn", edit.endColumn());
        payload.put("newText", edit.newText());
        return payload;
    }

    private static Map<String, Object> payload(Diagnostic diagnostic) {
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("startLine", diagnostic.startLine());
        range.put("startColumn", diagnostic.startColumn());
        range.put("endLine", diagnostic.endLine());
        range.put("endColumn", diagnostic.endColumn());
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("range", range);
        payload.put("severity", diagnostic.severity().name());
        payload.put("code", diagnostic.code());
        payload.put("message", diagnostic.message());
        payload.put("source", diagnostic.category());
        return payload;
    }

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int line(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.intValue() : 0;
    }

    private static long numberLong(Map<String, Object> payload, String key, long fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }
}
