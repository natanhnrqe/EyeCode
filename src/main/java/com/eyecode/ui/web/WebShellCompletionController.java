package com.eyecode.ui.web;

import com.eyecode.ui.web.monaco.MonacoModelId;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;
import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageId;
import com.eyecode.language.completion.CompletionCandidate;
import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.completion.CompletionService;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;

public final class WebShellCompletionController {
    private final WebShellSurface surface;
    private final EditorManager manager;
    private final ExecutorService executor;
    private final Map<String, String> latestRequestByUri = new ConcurrentHashMap<>();
    private final CompletionService completionService;
    private volatile boolean disposed;

    WebShellCompletionController(WebShellSurface surface, EditorManager manager, CompletionService completionService) {
        this.surface = surface;
        this.manager = manager;
        this.completionService = completionService;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "eyecode-web-completion");
            thread.setDaemon(true);
            return thread;
        });
        surface.registerHandler("completion", "request", this::request);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        latestRequestByUri.clear();
        executor.shutdownNow();
    }

    private WebShellEnvelope request(WebShellEnvelope message) {
        String modelId = text(message.payload(), "uri");
        if (modelId.isBlank()) modelId = text(message.payload(), "modelId");
        String requestModelId = modelId;
        latestRequestByUri.put(requestModelId, message.requestId());
        executor.execute(() -> compute(message, requestModelId));
        return acknowledgment(message, true);
    }

    private void compute(WebShellEnvelope message, String modelId) {
        if (disposed || !isLatest(modelId, message.requestId())) return;
        try {
            EditorSession session = sessionForModel(modelId);
            boolean lessonPractice = isLessonPracticeRequest(message.payload(), modelId);
            if (session == null && !lessonPractice) {
                publish(message, responsePayload(message, modelId, numberLong(message.payload(), "version", 0), List.of()));
                return;
            }
            var snapshot = session == null ? null : manager.getBuffer(session.getSessionId())
                    .map(buffer -> buffer.getDocument().snapshot()).orElse(null);
            String content = snapshot == null ? text(message.payload(), "content") : snapshot.getText();
            int offset = number(message.payload(), "offset", -1);
            if (offset < 0) {
                int line = number(message.payload(), "line", 1);
                int column = number(message.payload(), "column", 1);
                offset = com.eyecode.editor.intelligence.document.LineMap.of(content)
                        .offsetOf(Math.max(1, line), Math.max(1, column));
            }
            offset = Math.max(0, Math.min(offset, content.length()));
            LanguageDocument document = new LanguageDocument(modelId, session == null ? null : session.getFile(),
                    session == null ? text(message.payload(), "displayName") : session.getDisplayName(),
                    LanguageId.parse(text(message.payload(), "language")).orElse(null));
            long version = snapshot == null ? numberLong(message.payload(), "version", 0) : snapshot.version();
            CompletionResult result = completionService.complete(new CompletionRequest(document,
                    version, content, offset,
                    Boolean.TRUE.equals(message.payload().get("explicit")),
                    number(message.payload(), "replaceStart", -1), number(message.payload(), "replaceEnd", -1)));
            if (isLatest(modelId, message.requestId())) {
                publish(message, responsePayload(message, modelId, version, result.candidates()));
            }
        } catch (RuntimeException exception) {
            if (!isLatest(modelId, message.requestId())) return;
            WebShellEnvelope error = message.error(new WebShellError("COMPLETION_FAILED",
                    exception.getMessage() == null ? "Completion failed" : exception.getMessage(), true));
            surface.send(error);
        }
    }

    private boolean isLatest(String modelId, String requestId) {
        return requestId.equals(latestRequestByUri.get(modelId));
    }

    private WebShellEnvelope publish(WebShellEnvelope message, Map<String, Object> response) {
        surface.send(message.response(response));
        return acknowledgment(message, true);
    }

    private WebShellEnvelope acknowledgment(WebShellEnvelope message, boolean accepted) {
        return message.response(Map.of("accepted", accepted, "requestId", message.requestId()));
    }

    private Map<String, Object> responsePayload(WebShellEnvelope message, String modelId, long version,
                                                  List<CompletionCandidate> items) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (CompletionCandidate item : items) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("label", item.label());
            value.put("kind", item.kind());
            value.put("detail", item.detail());
            value.put("documentation", item.documentation());
            value.put("insertText", item.insertText());
            value.put("filterText", item.filterText());
            value.put("snippet", item.snippet());
            value.put("replaceStart", item.replaceStart());
            value.put("replaceEnd", item.replaceEnd());
            value.put("sortKey", item.sortKey());
            value.put("signature", item.signature());
            value.put("returnType", item.returnType());
            value.put("owner", item.owner());
            value.put("example", item.example());
            value.put("category", item.category());
            value.put("matchIndices", item.matchIndices());
            serialized.add(value);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", message.requestId());
        response.put("uri", modelId);
        response.put("version", version);
        response.put("items", serialized);
        return response;
    }

    private EditorSession sessionForModel(String modelId) {
        for (EditorSession session : manager.getSessions()) {
            if (MonacoModelId.matches(modelId, session.getFile())
                    || MonacoModelId.forSession(session).equals(modelId)) {
                return session;
            }
        }
        return null;
    }

    private static boolean isLessonPracticeRequest(Map<String, Object> payload, String uri) {
        return uri.startsWith("lesson://") && Boolean.TRUE.equals(payload.get("lessonPractice"))
                && payload.get("content") instanceof String;
    }

    private static String text(Map<String, Object> payload, String key) {
        Object value = payload == null ? null : payload.get(key);
        return value == null ? "" : String.valueOf(value);
    }

    private static int number(Map<String, Object> payload, String key, int fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.intValue() : fallback;
    }

    private static long numberLong(Map<String, Object> payload, String key, long fallback) {
        Object value = payload == null ? null : payload.get(key);
        return value instanceof Number number ? number.longValue() : fallback;
    }

}
