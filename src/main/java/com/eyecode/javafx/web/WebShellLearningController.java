package com.eyecode.javafx.web;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;
import com.eyecode.javafx.learning.MonacoLearningOverlayPayload;
import com.eyecode.language.documentation.DocumentationAtCaretResolver;
import com.eyecode.language.documentation.JavaJdkTypeCatalog;
import com.eyecode.language.documentation.JdkSourceResolver;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.language.semantic.JavaMemberTargetResolver;
import com.eyecode.javafx.monaco.MonacoModelId;
import com.eyecode.learning.catalog.JavaSyntaxLearningCatalog;
import com.eyecode.learning.catalog.JdkLearningConceptCatalog;
import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.content.LearningDocument;
import com.eyecode.learning.content.LearningMember;
import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class WebShellLearningController {
    private final JavaFxWebShellSurface surface;
    private final EditorManager manager;
    private final LearningContentEngine contentEngine = new LearningContentEngine();
    private final JavaSyntaxAnalyzer syntaxAnalyzer = new JavaSyntaxAnalyzer();
    private final JavaMemberTargetResolver memberTargetResolver = new JavaMemberTargetResolver();
    private final DocumentationAtCaretResolver jdkResolver = new DocumentationAtCaretResolver();
    private final JdkLearningConceptCatalog jdkCatalog = new JdkLearningConceptCatalog();
    private final JavaSyntaxLearningCatalog syntaxCatalog = new JavaSyntaxLearningCatalog();
    private final JdkSourceResolver sourceResolver = new JdkSourceResolver();

    public WebShellLearningController(JavaFxWebShellSurface surface, EditorManager manager) {
        this.surface = surface;
        this.manager = manager;
        surface.registerHandler("learning", "request", this::request);
        surface.registerHandler("learning", "close", message -> message.response(Map.of("accepted", true)));
    }

    private WebShellEnvelope request(WebShellEnvelope message) {
        try {
            String uri = text(message.payload(), "uri");
            EditorSession session = sessionFor(uri);
            if (session == null) return publish(message, response(message, uri, false, Map.of()));
            String content = text(message.payload(), "content");
            if (content.isEmpty()) {
                content = manager.getBuffer(session.getSessionId())
                        .map(buffer -> buffer.getDocument().snapshot().getText()).orElse("");
            }
            int offset = clamp(number(message.payload(), "offset", 0), content.length());
            Optional<LearningConcept> concept = conceptFor(message.payload(), session, content, offset);
            if (concept.isEmpty()) return publish(message, response(message, uri, false, Map.of()));
            Optional<Map<String, Object>> payload = payloadFor(concept.get());
            return publish(message, response(message, uri, payload.isPresent(), payload.orElse(Map.of())));
        } catch (RuntimeException exception) {
            surface.send(message.error(new WebShellError("LEARNING_FAILED",
                    exception.getMessage() == null ? "Learning request failed" : exception.getMessage(), true)));
            return acknowledgment(message, false);
        }
    }

    private Optional<LearningConcept> conceptFor(Map<String, Object> request, EditorSession session,
                                                 String content, int offset) {
        String identifier = text(request, "identifier");
        if (!identifier.isBlank()) return conceptForIdentifier(identifier);
        EditorDocument document = new EditorDocument(session.getFile(), content);
        Optional<SyntaxToken> token = syntaxAnalyzer.analyze(document).getTokens().stream()
                .filter(candidate -> candidate.startOffset() <= offset && offset < candidate.endOffset())
                .findFirst();
        if (token.isEmpty()) return Optional.empty();
        if (token.get().type() == TokenType.IDENTIFIER) {
            Optional<LearningConcept> member = memberTargetResolver.resolve(content, token.get().startOffset())
                    .flatMap(jdkCatalog::find);
            if (member.isPresent()) return member;
            Optional<LearningConcept> type = jdkResolver.resolveType(content, token.get().startOffset())
                    .flatMap(resolved -> jdkCatalog.find(resolved.simpleName()));
            if (type.isPresent()) return type;
        }
        return token.get().type() == TokenType.KEYWORD ? syntaxCatalog.find(token.get().text()) : Optional.empty();
    }

    private Optional<LearningConcept> conceptForIdentifier(String identifier) {
        try {
            LearningDocument document = contentEngine.loadDocument(identifier);
            LearningConcept concept = new LearningConcept();
            concept.setId(document.identifier());
            concept.setTitle(document.metadata().title());
            LearningPage page = new LearningPage(identifier);
            page.setId(identifier);
            concept.setPage(page);
            return Optional.of(concept);
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }

    private Optional<Map<String, Object>> payloadFor(LearningConcept concept) {
        if (concept.getPage() == null || concept.getPage().getId() == null) return Optional.empty();
        LearningDocument document;
        try {
            document = contentEngine.loadDocument(concept.getPage().getId());
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
        JdkSourceTarget sourceTarget = concept.getQualifiedName() == null ? null
                : JavaJdkTypeCatalog.findQualified(concept.getQualifiedName())
                        .flatMap(sourceResolver::resolve).orElse(null);
        if (document.metadata().sourceMember() != null && sourceTarget != null && sourceTarget.memberName() == null) {
            sourceTarget = sourceTarget.withMember(document.metadata().sourceMember());
        }
        LearningMetadata metadata = document.metadata();
        DocumentationTarget docs = documentationTarget(metadata);
        MonacoLearningOverlayPayload overlay = MonacoLearningOverlayPayload.from(metadata, ancestorsFor(metadata),
                bodyHtml(document.renderedHtml()), commonMethodsFor(metadata), relatedFor(metadata), docs,
                sourceTarget != null || sourceTarget(metadata) != null);
        return Optional.of(toMap(overlay));
    }

    private WebShellEnvelope publish(WebShellEnvelope message, Map<String, Object> response) {
        surface.send(message.response(response));
        return acknowledgment(message, true);
    }

    private WebShellEnvelope acknowledgment(WebShellEnvelope message, boolean accepted) {
        return message.response(Map.of("accepted", accepted, "requestId", message.requestId()));
    }

    private Map<String, Object> response(WebShellEnvelope message, String uri, boolean found,
                                         Map<String, Object> card) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", message.requestId());
        response.put("uri", uri);
        response.put("version", numberLong(message.payload(), "version", 0));
        response.put("found", found);
        response.put("card", card);
        return response;
    }

    private static Map<String, Object> toMap(MonacoLearningOverlayPayload payload) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("title", payload.title());
        value.put("subtitle", payload.subtitle());
        value.put("sizeClass", payload.sizeClass());
        value.put("iconKind", payload.iconKind());
        value.put("iconUrl", payload.iconUrl());
        value.put("breadcrumb", items(payload.breadcrumb()));
        value.put("renderedBodyHtml", payload.renderedBodyHtml());
        value.put("commonMethods", items(payload.commonMethods()));
        value.put("relatedItems", items(payload.relatedItems()));
        value.put("sourceAvailable", payload.sourceAvailable());
        value.put("docsAvailable", payload.docsAvailable());
        return value;
    }

    private static List<Map<String, Object>> items(List<MonacoLearningOverlayPayload.Item> items) {
        return items.stream().map(item -> Map.<String, Object>of("id", item.id(), "title", item.title())).toList();
    }

    private List<LearningMetadata> relatedFor(LearningMetadata metadata) {
        List<LearningMetadata> related = new ArrayList<>();
        for (String identifier : metadata.related()) {
            if (identifier.equals(metadata.parent())) continue;
            try {
                related.add(contentEngine.loadDocument(identifier).metadata());
            } catch (RuntimeException ignored) {
            }
        }
        return related;
    }

    private List<LearningMember> commonMethodsFor(LearningMetadata metadata) {
        return metadata.members().stream().filter(member -> canLoad(member.identifier())).toList();
    }

    private boolean canLoad(String identifier) {
        try {
            contentEngine.loadDocument(identifier);
            return true;
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private DocumentationTarget documentationTarget(LearningMetadata metadata) {
        LearningMetadata reference = referenceMetadata(metadata);
        return reference == null ? null : reference.officialDocs();
    }

    private JdkSourceTarget sourceTarget(LearningMetadata metadata) {
        LearningMetadata reference = referenceMetadata(metadata);
        if (reference == null || reference.officialDocs() == null) return null;
        return JavaJdkTypeCatalog.findSimple(reference.officialDocs().label())
                .flatMap(sourceResolver::resolve)
                .map(target -> target.withMember(metadata.sourceMember())).orElse(null);
    }

    private LearningMetadata referenceMetadata(LearningMetadata metadata) {
        if (metadata == null || metadata.officialDocs() != null || metadata.parent() == null) return metadata;
        try {
            return contentEngine.loadDocument(metadata.parent()).metadata();
        } catch (RuntimeException ignored) {
            return metadata;
        }
    }

    private List<LearningMetadata> ancestorsFor(LearningMetadata metadata) {
        List<LearningMetadata> ancestors = new ArrayList<>();
        Set<String> visited = new java.util.HashSet<>();
        String parent = metadata.parent();
        while (parent != null && visited.add(parent) && ancestors.size() < 32) {
            try {
                LearningMetadata ancestor = contentEngine.loadDocument(parent).metadata();
                ancestors.add(0, ancestor);
                parent = ancestor.parent();
            } catch (RuntimeException ignored) {
                break;
            }
        }
        return ancestors;
    }

    private EditorSession sessionFor(String uri) {
        return manager.getSessions().stream().filter(session -> MonacoModelId.forSession(session).equals(uri)
                || MonacoModelId.matches(uri, session.getFile())).findFirst().orElse(null);
    }

    private static String bodyHtml(String html) {
        if (html == null) return "";
        int start = html.indexOf("<body");
        start = start < 0 ? 0 : html.indexOf('>', start) + 1;
        int end = html.lastIndexOf("</body>");
        return end > start ? html.substring(start, end) : html;
    }

    private static int clamp(int value, int length) {
        return Math.max(0, Math.min(value, length));
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
