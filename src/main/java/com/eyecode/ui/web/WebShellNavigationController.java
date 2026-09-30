package com.eyecode.ui.web;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.language.navigation.JdtLsNavigationService;
import com.eyecode.language.navigation.NavigationService;
import com.eyecode.language.navigation.NavigationTarget;
import com.eyecode.language.semantic.DefinitionAtCaretResolver;
import com.eyecode.language.semantic.DefinitionLocation;
import com.eyecode.language.semantic.JavaReferenceFinder;
import com.eyecode.language.semantic.ReferenceLocation;
import com.eyecode.language.symbol.DocumentSemanticModelBuilder;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.ui.web.monaco.MonacoModelId;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class WebShellNavigationController {
    private final WebShellSurface surface;
    private final EditorManager manager;
    private final JdtLsProjectService jdt;
    private final DefinitionAtCaretResolver localDefinition = new DefinitionAtCaretResolver();
    private final JavaReferenceFinder localReferences = new JavaReferenceFinder();
    private final DocumentSemanticModelBuilder semanticModels = new DocumentSemanticModelBuilder();
    private volatile boolean disposed;

    WebShellNavigationController(WebShellSurface surface, EditorManager manager, JdtLsProjectService jdt) {
        this.surface = surface;
        this.manager = Objects.requireNonNull(manager, "manager must not be null");
        this.jdt = jdt;
        surface.registerHandler("navigation", "definition", this::definition);
        surface.registerHandler("navigation", "references", this::references);
    }

    public void dispose() {
        disposed = true;
    }

    private WebShellEnvelope definition(WebShellEnvelope message) {
        return handle(message, true);
    }

    private WebShellEnvelope references(WebShellEnvelope message) {
        return handle(message, false);
    }

    private WebShellEnvelope handle(WebShellEnvelope message, boolean definition) {
        Map<String, Object> payload = message.payload();
        String modelId = WebShellPayload.text(payload, "uri");
        if (disposed) {
            return message.response(nonePayload(message.requestId(), modelId));
        }
        EditorSession session = sessionForModel(modelId);
        DocumentSnapshot snapshot = session == null ? null : manager.getBuffer(session.getSessionId())
                .map(buffer -> buffer.getDocument().snapshot()).orElse(null);
        if (session == null || snapshot == null) {
            return message.response(nonePayload(message.requestId(), modelId));
        }
        try {
            String source = snapshot.getText();
            LineMap lines = LineMap.of(source);
            int line = Math.max(1, (int) WebShellPayload.number(payload, "line", 1));
            int column = Math.max(1, (int) WebShellPayload.number(payload, "column", 1));
            int offset = lines.offsetOf(line - 1, column - 1);
            boolean includeDeclaration = Boolean.TRUE.equals(payload.get("includeDeclaration"));
            Path file = session.getFile();

            List<NavigationTarget> targets = List.of();
            String origin = "none";
            Optional<NavigationService> navigation = jdt == null ? Optional.empty() : jdt.navigation();
            if (navigation.isPresent()) {
                NavigationService service = navigation.get();
                try {
                    targets = definition
                            ? service.definition(file, source, snapshot.version(), line - 1, column - 1)
                            : service.references(file, source, snapshot.version(), line - 1, column - 1, includeDeclaration);
                    if (!targets.isEmpty()) origin = "jdt";
                } catch (RuntimeException exception) {
                    targets = List.of();
                }
            }
            if (targets.isEmpty()) {
                targets = localTargets(definition, snapshot, offset, modelId, includeDeclaration);
                if (!targets.isEmpty()) origin = "local";
            }
            return message.response(responsePayload(message.requestId(), modelId,
                    MonacoModelId.forSession(session), targets, origin, source));
        } catch (RuntimeException exception) {
            return message.response(nonePayload(message.requestId(), modelId));
        }
    }

    private List<NavigationTarget> localTargets(boolean definition, DocumentSnapshot snapshot, int offset,
                                                 String modelId, boolean includeDeclaration) {
        SymbolTable table = semanticModels.build(snapshot)
                .map(SemanticModelSnapshot::symbolTable).orElse(null);
        if (table == null) return List.of();
        String source = snapshot.getText();
        if (definition) {
            return localDefinition.resolve(source, offset, table)
                    .<List<NavigationTarget>>map(location ->
                            List.of(NavigationTarget.of(modelId, location.declarationRange())))
                    .orElse(List.of());
        }
        Optional<DefinitionLocation> declaration = localDefinition.resolve(source, offset, table);
        if (declaration.isEmpty()) return List.of();
        Symbol symbol = declaration.get().symbol();
        List<NavigationTarget> targets = new ArrayList<>();
        if (includeDeclaration) {
            targets.add(NavigationTarget.of(modelId, symbol.declarationRange()));
        }
        for (ReferenceLocation reference : localReferences.findReferences(symbol, table)) {
            targets.add(NavigationTarget.of(modelId, reference.range()));
        }
        return List.copyOf(targets);
    }

    private Map<String, Object> responsePayload(String requestId, String modelId, String canonicalUri,
                                                List<NavigationTarget> targets, String origin, String requestSource) {
        List<Map<String, Object>> serialized = new ArrayList<>();
        for (NavigationTarget target : targets) {
            serialized.add(targetPayload(target, modelId, canonicalUri, requestSource));
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", requestId);
        response.put("uri", modelId);
        response.put("targets", serialized);
        response.put("source", origin);
        return response;
    }

    private static Map<String, Object> nonePayload(String requestId, String modelId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", requestId);
        response.put("uri", modelId);
        response.put("targets", List.of());
        response.put("source", "none");
        return response;
    }

    private Map<String, Object> targetPayload(NavigationTarget target, String modelId, String canonicalUri,
                                              String requestSource) {
        String content = contentFor(target.uri(), modelId, canonicalUri, requestSource);
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uri", target.uri());
        payload.put("range", rangePayload(target.range(), content));
        payload.put("selectionRange", rangePayload(target.selectionRange(), content));
        return payload;
    }

    private static String contentFor(String targetUri, String modelId, String canonicalUri, String requestSource) {
        if (requestSource != null && canonicalUri != null && targetUri.equals(canonicalUri)) {
            return requestSource;
        }
        return JdtLsNavigationService.contentOf(targetUri, modelId, requestSource);
    }

    private static Map<String, Object> rangePayload(TextRange range, String content) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (content == null) {
            payload.put("startLine", 1);
            payload.put("startColumn", 1);
            payload.put("endLine", 1);
            payload.put("endColumn", 1);
            return payload;
        }
        LineMap lines = LineMap.of(content);
        int start = Math.max(0, Math.min(range.startOffset(), content.length()));
        int end = Math.max(start, Math.min(range.endOffset(), content.length()));
        payload.put("startOffset", start);
        payload.put("endOffset", end);
        payload.put("startLine", lines.lineOfOffset(start) + 1);
        payload.put("startColumn", lines.columnOfOffset(start) + 1);
        payload.put("endLine", lines.lineOfOffset(end) + 1);
        payload.put("endColumn", lines.columnOfOffset(end) + 1);
        return payload;
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
}
