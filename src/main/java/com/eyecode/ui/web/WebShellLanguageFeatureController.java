package com.eyecode.ui.web;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import com.eyecode.language.hover.HoverContent;
import com.eyecode.language.hover.HoverResult;
import com.eyecode.language.hover.HoverService;
import com.eyecode.language.inlay.InlayHint;
import com.eyecode.language.inlay.InlayHintMode;
import com.eyecode.language.inlay.InlayHintRequest;
import com.eyecode.language.inlay.InlayHintResult;
import com.eyecode.language.inlay.InlayHintService;
import com.eyecode.language.signature.SignatureHelpResult;
import com.eyecode.language.signature.SignatureHelpService;
import com.eyecode.language.signature.SignatureInformation;
import com.eyecode.language.signature.SignatureParameter;
import com.eyecode.ui.web.monaco.MonacoModelId;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class WebShellLanguageFeatureController {
    private final WebShellSurface surface;
    private final EditorManager manager;
    private final HoverService hoverService;
    private final SignatureHelpService signatureHelpService;
    private final InlayHintService inlayHintService;
    private final ExecutorService executor;
    private final Map<String, String> latestRequest = new ConcurrentHashMap<>();
    private volatile boolean disposed;

    public WebShellLanguageFeatureController(WebShellSurface surface, EditorManager manager,
                                             HoverService hoverService, SignatureHelpService signatureHelpService,
                                             InlayHintService inlayHintService) {
        this.surface = surface;
        this.manager = manager;
        this.hoverService = hoverService;
        this.signatureHelpService = signatureHelpService;
        this.inlayHintService = inlayHintService;
        this.executor = Executors.newSingleThreadExecutor(runnable -> {
            Thread thread = new Thread(runnable, "eyecode-web-language-feature");
            thread.setDaemon(true);
            return thread;
        });
        surface.registerHandler("hover", "request", message -> request(message, "hover"));
        surface.registerHandler("signatureHelp", "request", message -> request(message, "signatureHelp"));
        surface.registerHandler("inlayHints", "request", this::requestInlay);
    }

    public void dispose() {
        if (disposed) return;
        disposed = true;
        latestRequest.clear();
        executor.shutdownNow();
    }

    private WebShellEnvelope requestInlay(WebShellEnvelope message) {
        String modelId = text(message.payload(), "uri");
        if (modelId.isBlank()) modelId = text(message.payload(), "modelId");
        System.out.println("[LANG-FEATURE] inlayHints request uri=" + modelId
                + " mode=" + text(message.payload(), "mode")
                + " range=[" + number(message.payload(), "fromOffset", 0)
                + "," + number(message.payload(), "toOffset", 0) + "]");
        String key = "inlayHints\u0000" + modelId;
        latestRequest.put(key, message.requestId());
        String requestModelId = modelId;
        executor.execute(() -> computeInlay(message, requestModelId, key));
        return message.response(Map.of("accepted", true, "requestId", message.requestId()));
    }

    private void computeInlay(WebShellEnvelope message, String modelId, String key) {
        if (disposed || !isLatest(key, message.requestId())) return;
        try {
            EditorSession session = sessionForModel(modelId);
            var snapshot = session == null ? null : manager.getBuffer(session.getSessionId())
                    .map(buffer -> buffer.getDocument().snapshot()).orElse(null);
            if (session == null || snapshot == null) {
                System.out.println("[LANG-FEATURE] inlayHints no session uri=" + modelId);
                publish(message, "inlayHints", modelId, numberLong(message.payload(), "version", 0),
                        inlayPayload(null));
                return;
            }
            LanguageDocument document = new LanguageDocument(modelId, session.getFile(), session.getDisplayName(),
                    LanguageId.parse(text(message.payload(), "language")).orElse(null));
            InlayHintRequest request = new InlayHintRequest(document, snapshot.version(), snapshot.getText(),
                    number(message.payload(), "fromOffset", 0), number(message.payload(), "toOffset", 0),
                    InlayHintMode.parse(text(message.payload(), "mode")));
            InlayHintResult result = inlayHintService.inlayHints(request).orElse(null);
            System.out.println("[LANG-FEATURE] inlayHints result hints="
                    + (result == null ? 0 : result.hints().size()));
            if (isLatest(key, message.requestId())) {
                publish(message, "inlayHints", modelId, snapshot.version(), inlayPayload(result));
            }
        } catch (RuntimeException exception) {
            if (isLatest(key, message.requestId())) {
                surface.send(message.error(new WebShellError("LANGUAGE_FEATURE_FAILED",
                        exception.getMessage() == null ? "Language feature failed" : exception.getMessage(), true)));
            }
        }
    }

    private static Map<String, Object> inlayPayload(InlayHintResult result) {
        if (result == null) return Map.of("hints", List.of());
        return Map.of("hints", result.hints().stream()
                .map(WebShellLanguageFeatureController::hintPayload).toList());
    }

    private static Map<String, Object> hintPayload(InlayHint hint) {
        return Map.of("offset", hint.offset(), "label", hint.label());
    }

    private WebShellEnvelope request(WebShellEnvelope message, String feature) {
        String modelId = text(message.payload(), "uri");
        if (modelId.isBlank()) modelId = text(message.payload(), "modelId");
        System.out.println("[LANG-FEATURE] " + feature + " request uri=" + modelId
                + " offset=" + number(message.payload(), "offset", 0));
        String key = feature + "\u0000" + modelId;
        latestRequest.put(key, message.requestId());
        String requestModelId = modelId;
        executor.execute(() -> compute(message, feature, requestModelId, key));
        return message.response(Map.of("accepted", true, "requestId", message.requestId()));
    }

    private void compute(WebShellEnvelope message, String feature, String modelId, String key) {
        if (disposed || !isLatest(key, message.requestId())) return;
        try {
            EditorSession session = sessionForModel(modelId);
            var snapshot = session == null ? null : manager.getBuffer(session.getSessionId())
                    .map(buffer -> buffer.getDocument().snapshot()).orElse(null);
            if (session == null || snapshot == null) {
                System.out.println("[LANG-FEATURE] " + feature + " no session uri=" + modelId);
                Map<String, Object> empty = feature.equals("hover")
                        ? hoverPayload(null) : signaturePayload((SignatureHelpResult) null);
                publish(message, feature, modelId, numberLong(message.payload(), "version", 0), empty);
                return;
            }
            LanguageDocument document = new LanguageDocument(modelId, session.getFile(), session.getDisplayName(),
                    LanguageId.parse(text(message.payload(), "language")).orElse(null));
            LanguageFeatureRequest request = new LanguageFeatureRequest(document, snapshot.version(),
                    snapshot.getText(), number(message.payload(), "offset", 0),
                    text(message.payload(), "triggerCharacter"));
            Map<String, Object> result = feature.equals("hover")
                    ? hoverPayload(hoverService.hover(request).orElse(null))
                    : signaturePayload(signatureHelpService.signatureHelp(request).orElse(null));
            System.out.println("[LANG-FEATURE] " + feature + " result " + featureSummary(feature, result));
            if (isLatest(key, message.requestId())) publish(message, feature, modelId, snapshot.version(), result);
        } catch (RuntimeException exception) {
            if (isLatest(key, message.requestId())) {
                surface.send(message.error(new WebShellError("LANGUAGE_FEATURE_FAILED",
                        exception.getMessage() == null ? "Language feature failed" : exception.getMessage(), true)));
            }
        }
    }

    private void publish(WebShellEnvelope message, String feature, String modelId, long version,
                         Map<String, Object> result) {
        Map<String, Object> response = new LinkedHashMap<>(result);
        response.put("requestId", message.requestId());
        response.put("feature", feature);
        response.put("uri", modelId);
        response.put("version", version);
        surface.send(message.response(response));
    }

    private boolean isLatest(String key, String requestId) {
        return requestId.equals(latestRequest.get(key));
    }

    private EditorSession sessionForModel(String modelId) {
        for (EditorSession session : manager.getSessions()) {
            if (MonacoModelId.matches(modelId, session.getFile()) || MonacoModelId.forSession(session).equals(modelId)) {
                return session;
            }
        }
        return null;
    }

    private static String featureSummary(String feature, Map<String, Object> result) {
        Object value = result.get(feature.equals("hover") ? "contents" : "signatures");
        if (!(value instanceof List<?> list)) return feature.equals("hover") ? "contents=0" : "signatures=0";
        if (!feature.equals("hover")) return "signatures=" + list.size();
        StringBuilder kinds = new StringBuilder();
        for (Object item : list) {
            if (item instanceof Map<?, ?> content) {
                if (kinds.length() > 0) kinds.append(',');
                kinds.append(content.get("kind"));
            }
        }
        return "contents=" + list.size() + " kinds=[" + kinds + "]";
    }

    private static Map<String, Object> hoverPayload(HoverResult result) {
        if (result == null) return Map.of("contents", List.of());
        List<Map<String, Object>> contents = result.contents().stream().map(WebShellLanguageFeatureController::contentPayload).toList();
        return Map.of("contents", contents, "rangeStart", result.rangeStart(), "rangeEnd", result.rangeEnd());
    }

    private static Map<String, Object> contentPayload(HoverContent content) {
        return Map.of("kind", content.kind(), "value", content.value());
    }

    private static Map<String, Object> signaturePayload(SignatureHelpResult result) {
        if (result == null) return Map.of("signatures", List.of());
        return Map.of("signatures", result.signatures().stream()
                        .map(WebShellLanguageFeatureController::signaturePayload).toList(),
                "activeSignature", result.activeSignature() == null ? -1 : result.activeSignature(),
                "activeParameter", result.activeParameter() == null ? -1 : result.activeParameter());
    }

    private static Map<String, Object> signaturePayload(SignatureInformation signature) {
        List<Map<String, Object>> parameters = signature.parameters().stream()
                .map(WebShellLanguageFeatureController::parameterPayload).toList();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("label", signature.label());
        payload.put("documentation", signature.documentation());
        payload.put("parameters", parameters);
        payload.put("activeParameter", signature.activeParameter() == null ? -1 : signature.activeParameter());
        return payload;
    }

    private static Map<String, Object> parameterPayload(SignatureParameter parameter) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("label", parameter.label());
        payload.put("documentation", parameter.documentation());
        if (parameter.labelStart() != null) payload.put("labelStart", parameter.labelStart());
        if (parameter.labelEnd() != null) payload.put("labelEnd", parameter.labelEnd());
        return payload;
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
