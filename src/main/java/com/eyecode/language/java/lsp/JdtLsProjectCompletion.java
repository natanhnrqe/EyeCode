package com.eyecode.language.java.lsp;

import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.completion.CompletionCandidate;
import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.hover.HoverContent;
import com.eyecode.language.hover.HoverResult;
import com.eyecode.language.signature.SignatureHelpResult;
import com.eyecode.language.signature.SignatureInformation;
import com.eyecode.language.signature.SignatureParameter;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionItemKind;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.MarkedString;
import org.eclipse.lsp4j.ParameterInformation;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Tuple;

import java.nio.file.Path;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JdtLsProjectCompletion {
    private final JdtLsSession session;
    private final Map<String, Integer> openedVersions = new HashMap<>();
    private final Map<String, String> openedTexts = new HashMap<>();

    public JdtLsProjectCompletion(JdtLsSession session) {
        this.session = session;
    }

    public synchronized Optional<CompletionResult> complete(CompletionRequest request, Duration timeout) {
        Path file = request.document().sourceFile();
        if (file == null || session.state() != JdtLsLifecycleState.READY) return Optional.empty();
        try {
            String uri = synchronize(file, request.source(), request.version());
            List<CompletionItem> items = session.completion(uri, positionFor(request.source(), request.caretOffset()).getLine(),
                    positionFor(request.source(), request.caretOffset()).getCharacter(), timeout);
            return Optional.of(new CompletionResult(items.stream().limit(100)
                    .map(item -> candidate(item, request)).toList()));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public synchronized Optional<HoverResult> hover(LanguageFeatureRequest request, Duration timeout) {
        Path file = request.document().sourceFile();
        if (file == null || session.state() != JdtLsLifecycleState.READY || !session.supportsHover()) {
            return Optional.empty();
        }
        String uri = synchronize(file, request.source(), request.version());
        try {
            Position position = positionFor(request.source(), request.caretOffset());
            Hover hover = session.hover(uri, position.getLine(), position.getCharacter(), timeout);
            return hover == null ? Optional.empty() : Optional.of(toHoverResult(hover, request));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public synchronized Optional<SignatureHelpResult> signatureHelp(LanguageFeatureRequest request, Duration timeout) {
        Path file = request.document().sourceFile();
        if (file == null || session.state() != JdtLsLifecycleState.READY || !session.supportsSignatureHelp()) {
            return Optional.empty();
        }
        String uri = synchronize(file, request.source(), request.version());
        try {
            Position position = positionFor(request.source(), request.caretOffset());
            SignatureHelp help = session.signatureHelp(uri, position.getLine(), position.getCharacter(),
                    request.triggerCharacter(), timeout);
            return help == null ? Optional.empty() : Optional.of(toSignatureHelpResult(help));
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    static Position positionFor(String source, int offset) {
        LineMap lines = LineMap.of(source);
        return new Position(lines.lineOfOffset(offset), lines.columnOfOffset(offset));
    }

    private String synchronize(Path file, String source, long version) {
        String uri = file.toAbsolutePath().normalize().toUri().toString();
        Integer previous = openedVersions.get(uri);
        if (previous == null) {
            int lspVersion = lspVersion(version);
            session.didOpen(uri, source, lspVersion);
            openedVersions.put(uri, lspVersion);
        } else if (!source.equals(openedTexts.get(uri))) {
            int lspVersion = Math.max(previous + 1, lspVersion(version));
            session.didChange(uri, source, lspVersion);
            openedVersions.put(uri, lspVersion);
        }
        openedTexts.put(uri, source);
        return uri;
    }

    static HoverResult toHoverResult(Hover hover, LanguageFeatureRequest request) {
        List<HoverContent> contents = new java.util.ArrayList<>();
        Either<List<Either<String, MarkedString>>, org.eclipse.lsp4j.MarkupContent> value = hover.getContents();
        if (value != null && value.isRight()) {
            var markup = value.getRight();
            contents.add(new HoverContent(markup.getKind(), sanitizeMarkup(markup.getValue())));
        } else if (value != null && value.isLeft()) {
            for (Either<String, MarkedString> item : value.getLeft()) {
                if (item.isLeft()) contents.add(new HoverContent("plaintext", item.getLeft()));
                else contents.add(new HoverContent(item.getRight().getLanguage(), item.getRight().getValue()));
            }
        }
        int start = request.caretOffset();
        int end = start;
        if (hover.getRange() != null) {
            LineMap lines = LineMap.of(request.source());
            start = lines.offsetOf(hover.getRange().getStart().getLine(), hover.getRange().getStart().getCharacter());
            end = lines.offsetOf(hover.getRange().getEnd().getLine(), hover.getRange().getEnd().getCharacter());
        }
        return new HoverResult(contents, start, end);
    }

    static String sanitizeMarkup(String value) {
        if (value == null || value.isEmpty()) return "";
        String sanitized = value.replaceAll("(?is)<script\\b[^>]*>.*?</script>", "");
        sanitized = sanitized.replaceAll("(?i)javascript:", "");
        sanitized = sanitized.replaceAll("(?i)\\son\\w+\\s*=", "");
        return sanitized;
    }

    static SignatureHelpResult toSignatureHelpResult(SignatureHelp help) {
        List<SignatureInformation> signatures = help.getSignatures() == null ? List.of() : help.getSignatures().stream()
                .map(signature -> new SignatureInformation(signature.getLabel(), text(signature.getDocumentation()),
                        parameters(signature.getParameters()), signature.getActiveParameter()))
                .toList();
        return new SignatureHelpResult(signatures, help.getActiveSignature(), help.getActiveParameter());
    }

    private static List<SignatureParameter> parameters(List<ParameterInformation> parameters) {
        if (parameters == null) return List.of();
        return parameters.stream().map(parameter -> {
            Either<String, Tuple.Two<Integer, Integer>> label = parameter.getLabel();
            if (label == null) return new SignatureParameter("", text(parameter.getDocumentation()));
            if (label.isLeft()) return new SignatureParameter(label.getLeft(), text(parameter.getDocumentation()));
            Tuple.Two<Integer, Integer> range = label.getRight();
            return new SignatureParameter("", text(parameter.getDocumentation()), range.getFirst(), range.getSecond());
        }).toList();
    }

    private static String text(Either<String, org.eclipse.lsp4j.MarkupContent> value) {
        if (value == null) return "";
        return value.isLeft() ? value.getLeft() : value.getRight().getValue();
    }

    public synchronized void close(Path file) {
        if (file == null) return;
        String uri = file.toAbsolutePath().normalize().toUri().toString();
        openedTexts.remove(uri);
        if (openedVersions.remove(uri) != null) session.didClose(uri);
    }

    static int lspVersion(long version) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, version + 1));
    }

    private static CompletionCandidate candidate(CompletionItem item, CompletionRequest request) {
        String label = item.getLabel();
        String insert = item.getInsertText();
        if (insert == null || insert.isBlank()) insert = label;
        String kind = kind(item.getKind());
        boolean method = "METHOD".equals(kind) || "CONSTRUCTOR".equals(kind);
        String name = methodName(label);
        if (method) {
            insert = name + "()";
        }
        boolean snippet = !method && item.getInsertTextFormat() != null && item.getInsertTextFormat().getValue() == 2;
        String detail = item.getDetail() != null ? item.getDetail() : "";
        String doc = documentation(item);
        String returnType = returnType(detail);
        String owner = owner(detail, label);
        String signature = method ? signatureFor(name, detail, label) : label;
        int priority = method ? 60 : "FIELD".equals(kind) ? 70 : 50;

        return new CompletionCandidate(label, kind, detail, doc, insert,
                item.getFilterText(), snippet, request.replaceStart() >= 0 ? request.replaceStart() : request.caretOffset(),
                request.replaceEnd() >= 0 ? request.replaceEnd() : request.caretOffset(), priority, signature,
                returnType, owner, "", "", List.of());
    }

    private static String signatureFor(String name, String detail, String label) {
        if (detail.contains("(") && detail.contains(")")) {
            return detail;
        }
        if (label.contains("(") && label.contains(")")) {
            return label;
        }
        return name + "()";
    }

    private static String methodName(String label) {
        int open = label.indexOf('(');
        return open > 0 ? label.substring(0, open).trim() : label.trim();
    }

    private static String documentation(CompletionItem item) {
        if (item.getDocumentation() == null) return "";
        return item.getDocumentation().isLeft() ? item.getDocumentation().getLeft()
                : item.getDocumentation().getRight().getValue();
    }

    private static String returnType(String detail) {
        if (detail == null) return "";
        int separator = detail.lastIndexOf(" : ");
        return separator < 0 ? "" : detail.substring(separator + 3).trim();
    }

    private static String owner(String detail, String label) {
        if (detail == null || label == null) return "";
        int method = detail.indexOf(methodName(label));
        return method <= 0 ? "" : detail.substring(0, method).trim().replaceAll("[. ]+$", "");
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
