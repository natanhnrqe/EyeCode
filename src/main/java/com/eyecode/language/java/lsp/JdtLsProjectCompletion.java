package com.eyecode.language.java.lsp;

import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.language.completion.CompletionCandidate;
import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Position;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JdtLsProjectCompletion {
    private final JdtLsSession session;
    private final Map<String, Integer> openedVersions = new HashMap<>();

    public JdtLsProjectCompletion(JdtLsSession session) {
        this.session = session;
    }

    public synchronized Optional<CompletionResult> complete(CompletionRequest request, Duration timeout) {
        Path file = request.document().sourceFile();
        if (file == null || session.state() != JdtLsLifecycleState.READY) return Optional.empty();
        String uri = file.toAbsolutePath().normalize().toUri().toString();
        int version = lspVersion(request.version());
        try {
            Integer previous = openedVersions.get(uri);
            if (previous == null) {
                session.didOpen(uri, request.source(), version);
            } else if (version > previous) {
                session.didChange(uri, request.source(), version);
            }
            openedVersions.put(uri, version);
            List<CompletionItem> items = session.completion(uri, positionFor(request.source(), request.caretOffset()).getLine(),
                    positionFor(request.source(), request.caretOffset()).getCharacter(), timeout);
            return Optional.of(new CompletionResult(items.stream().limit(100)
                    .map(item -> candidate(item, request)).toList()));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    static Position positionFor(String source, int offset) {
        LineMap lines = LineMap.of(source);
        return new Position(lines.lineOfOffset(offset), lines.columnOfOffset(offset));
    }

    public synchronized void close(Path file) {
        if (file == null) return;
        String uri = file.toAbsolutePath().normalize().toUri().toString();
        if (openedVersions.remove(uri) != null) session.didClose(uri);
    }

    private static int lspVersion(long version) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, version));
    }

    private static CompletionCandidate candidate(CompletionItem item, CompletionRequest request) {
        String label = item.getLabel();
        String insert = item.getInsertText();
        if (insert == null || insert.isBlank()) insert = label;
        boolean snippet = item.getInsertTextFormat() != null && item.getInsertTextFormat().getValue() == 2;
        return new CompletionCandidate(label, kind(item.getKind()), item.getDetail(), "", insert,
                item.getFilterText(), snippet, request.replaceStart() >= 0 ? request.replaceStart() : request.caretOffset(),
                request.replaceEnd() >= 0 ? request.replaceEnd() : request.caretOffset(), 0, label, "", "", "", "", List.of());
    }

    private static String kind(CompletionItemKind kind) {
        if (kind == null) return "VARIABLE";
        return switch (kind) {
            case Method, Function -> "METHOD";
            case Constructor -> "CONSTRUCTOR";
            case Field, Property -> "FIELD";
            case Variable -> "VARIABLE";
            case Class -> "CLASS";
            case Interface -> "INTERFACE";
            case Enum -> "ENUM";
            case Keyword -> "KEYWORD";
            case Snippet -> "SNIPPET";
            case Module -> "PACKAGE";
            case Constant -> "CONSTANT";
            default -> "VARIABLE";
        };
    }
}
