package com.eyecode.javafx.web;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.JavaSnippetProvider;
import com.eyecode.editor.v2.completion.JavaStandardLibraryProvider;
import com.eyecode.editor.v2.completion.knowledge.JavaKnowledgeBaseProvider;
import com.eyecode.editor.v2.completion.semantic.JavaSemanticMemberCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticSymbolRegistry;
import com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.javafx.monaco.EyeCodeCompletionService;
import com.eyecode.javafx.monaco.MonacoCompletionItem;
import com.eyecode.javafx.monaco.MonacoCompletionRequest;
import com.eyecode.javafx.monaco.MonacoModelId;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebShellCompletionController {
    private final JavaFxWebShellSurface surface;
    private final EditorManager manager;
    private final JavaSyntaxAnalyzer syntaxAnalyzer = new JavaSyntaxAnalyzer();
    private final EyeCodeCompletionService completionService = new EyeCodeCompletionService(
            new CompletionEngine(List.of(
                    new JavaKeywordCompletionProvider(),
                    new JavaSemanticMemberCompletionProvider(),
                    new JavaKnowledgeBaseProvider(),
                    new JavaStandardLibraryProvider(),
                    new JavaSnippetProvider(),
                    new SemanticCompletionProvider(new SemanticSymbolRegistry())
            )));

    public WebShellCompletionController(JavaFxWebShellSurface surface, EditorManager manager) {
        this.surface = surface;
        this.manager = manager;
        surface.registerHandler("completion", "request", this::request);
    }

    private WebShellEnvelope request(WebShellEnvelope message) {
        try {
            String modelId = text(message.payload(), "uri");
            if (modelId.isBlank()) modelId = text(message.payload(), "modelId");
            EditorSession session = sessionForModel(modelId);
            if (session == null) {
                return publish(message, responsePayload(message, modelId, List.of()));
            }
            String content = text(message.payload(), "content");
            if (content.isEmpty()) {
                content = manager.getBuffer(session.getSessionId())
                        .map(buffer -> buffer.getDocument().snapshot().getText()).orElse("");
            }
            EditorDocument document = new EditorDocument(session.getFile(), content);
            int offset = number(message.payload(), "offset", -1);
            if (offset < 0) {
                int line = number(message.payload(), "line", 1);
                int column = number(message.payload(), "column", 1);
                offset = document.offsetOf(new EditorPosition(Math.max(1, line), Math.max(1, column)));
            }
            offset = Math.max(0, Math.min(offset, content.length()));
            EditorPosition caret = document.positionOf(offset);
            LanguageContext context = new LanguageContext(
                    document,
                    caret,
                    new EditorSelection(caret, caret),
                    syntaxAnalyzer.analyze(document),
                    DiagnosticSnapshot.empty());
            MonacoCompletionRequest completionRequest = toRequest(message, modelId, content, offset);
            List<MonacoCompletionItem> items = completionService.complete(completionRequest, context);
            return publish(message, responsePayload(message, modelId, items));
        } catch (RuntimeException exception) {
            WebShellEnvelope error = message.error(new WebShellError("COMPLETION_FAILED",
                    exception.getMessage() == null ? "Completion failed" : exception.getMessage(), true));
            surface.send(error);
            return acknowledgment(message, false);
        }
    }

    private WebShellEnvelope publish(WebShellEnvelope message, Map<String, Object> response) {
        surface.send(message.response(response));
        return acknowledgment(message, true);
    }

    private WebShellEnvelope acknowledgment(WebShellEnvelope message, boolean accepted) {
        return message.response(Map.of("accepted", accepted, "requestId", message.requestId()));
    }

    private MonacoCompletionRequest toRequest(WebShellEnvelope message, String modelId,
                                              String content, int offset) {
        Map<String, Object> payload = message.payload();
        MonacoCompletionRequest.TriggerKind trigger = switch (text(payload, "triggerKind")) {
            case "triggerCharacter" -> MonacoCompletionRequest.TriggerKind.TRIGGER_CHARACTER;
            case "incomplete" -> MonacoCompletionRequest.TriggerKind.INCOMPLETE;
            default -> MonacoCompletionRequest.TriggerKind.INVOKED;
        };
        return new MonacoCompletionRequest(
                modelId,
                numberLong(payload, "version", 0),
                number(payload, "line", 1),
                number(payload, "column", 1),
                trigger,
                text(payload, "triggerCharacter"),
                numberLong(message.requestId(), 0),
                Boolean.TRUE.equals(payload.get("explicit")),
                offset,
                number(payload, "replaceStart", -1),
                number(payload, "replaceEnd", -1),
                content);
    }

    private Map<String, Object> responsePayload(WebShellEnvelope message, String modelId,
                                                  List<MonacoCompletionItem> items) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (MonacoCompletionItem item : items) {
            Map<String, Object> value = new LinkedHashMap<>();
            value.put("label", item.label());
            value.put("kind", item.kind().name());
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
            serialized.add(value);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", message.requestId());
        response.put("uri", modelId);
        response.put("version", numberLong(message.payload(), "version", 0));
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

    private static long numberLong(String value, long fallback) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }
}
