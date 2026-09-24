package com.eyecode.language.java.completion;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionContextKind;
import com.eyecode.editor.v2.completion.CompletionContextResolver;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.JavaSnippetProvider;
import com.eyecode.editor.v2.completion.JavaStandardLibraryProvider;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.completion.knowledge.JavaKnowledgeBaseProvider;
import com.eyecode.editor.v2.completion.semantic.JavaSemanticMemberCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticSymbolRegistry;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.language.LanguageId;
import com.eyecode.language.completion.CompletionCandidate;
import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.completion.CompletionProvider;
import com.eyecode.language.java.lsp.JdtLsProjectService;

import java.util.List;
import java.util.Optional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.IdentityHashMap;
import java.util.Map;

public final class JavaCompletionProvider implements CompletionProvider {
    private final JavaSyntaxAnalyzer syntaxAnalyzer;
    private final CompletionEngine engine;
    private final JdtLsProjectService jdt;

    public JavaCompletionProvider() {
        this(new JavaSyntaxAnalyzer(), new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new JavaSemanticMemberCompletionProvider(),
                new JavaKnowledgeBaseProvider(),
                new JavaStandardLibraryProvider(),
                new JavaSnippetProvider(),
                new SemanticCompletionProvider(new SemanticSymbolRegistry())
        )), null);
    }

    JavaCompletionProvider(JavaSyntaxAnalyzer syntaxAnalyzer, CompletionEngine engine) {
        this(syntaxAnalyzer, engine, null);
    }

    public JavaCompletionProvider(JdtLsProjectService jdt) {
        this(new JavaSyntaxAnalyzer(), new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(), new JavaSemanticMemberCompletionProvider(),
                new JavaKnowledgeBaseProvider(), new JavaStandardLibraryProvider(), new JavaSnippetProvider(),
                new SemanticCompletionProvider(new SemanticSymbolRegistry())
        )), jdt);
    }

    JavaCompletionProvider(JavaSyntaxAnalyzer syntaxAnalyzer, CompletionEngine engine, JdtLsProjectService jdt) {
        this.syntaxAnalyzer = syntaxAnalyzer;
        this.engine = engine;
        this.jdt = jdt;
    }

    @Override
    public LanguageId languageId() {
        return LanguageId.JAVA;
    }

    @Override
    public CompletionResult complete(CompletionRequest request) {
        EditorDocument document = new EditorDocument(request.document().sourceFile(), request.source());
        EditorPosition caret = document.positionOf(request.caretOffset());
        LanguageContext context = new LanguageContext(document, caret, new EditorSelection(caret, caret),
                syntaxAnalyzer.analyze(document), com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot.empty());
        var snapshot = engine.complete(context, request.explicit()
                || request.triggerKind() == CompletionRequest.TriggerKind.TRIGGER_CHARACTER);
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        int start = request.replaceStart() >= 0 && request.replaceStart() <= request.caretOffset()
                ? request.replaceStart() : Math.max(0, request.caretOffset() - prefix.length());
        int end = request.replaceEnd() >= request.caretOffset() ? request.replaceEnd() : request.caretOffset();
        int boundedStart = Math.max(0, Math.min(start, request.source().length()));
        int boundedEnd = Math.max(boundedStart, Math.min(end, request.source().length()));
        CompletionResult local = new CompletionResult(snapshot.getItems().stream()
                .limit(100)
                .map(item -> candidate(item, boundedStart, boundedEnd, prefix)).toList());
        CompletionContextKind contextKind = CompletionContextResolver.resolve(context);
        if (contextKind != CompletionContextKind.MEMBER_ACCESS || jdt == null) {
            return local;
        }
        Optional<CompletionResult> semantic = jdt.complete(request);
        if (semantic.isEmpty()) return local;
        return mergeMemberResults(local, semantic.get(), prefix, boundedStart, boundedEnd);
    }

    static CompletionResult selectJdtOrFallback(Optional<CompletionResult> jdtResult,
                                                CompletionResult fallback) {
        return jdtResult.filter(result -> !result.candidates().isEmpty()).orElse(fallback);
    }

    static CompletionResult mergeMemberResults(CompletionResult local, CompletionResult semantic,
                                               String prefix) {
        return mergeMemberResults(local, semantic, prefix, 0, 0);
    }

    static CompletionResult mergeMemberResults(CompletionResult local, CompletionResult semantic,
                                               String prefix, int replaceStart, int replaceEnd) {
        List<CompletionCandidate> merged = new ArrayList<>(local.candidates());
        Set<String> identities = new HashSet<>(local.candidates().stream()
                .map(JavaCompletionProvider::identity).toList());
        for (CompletionCandidate candidate : semantic.candidates()) {
            if (!prefix.isEmpty() && !isSubsequence(prefix, baseLabel(candidate.label()))) continue;
            if (identities.add(identity(candidate))) {
                merged.add(enrichCandidate(candidate, prefix, replaceStart, replaceEnd));
            }
        }
        com.eyecode.editor.v2.completion.CompletionRanking ranking =
                new com.eyecode.editor.v2.completion.CompletionRanking();
        Map<CompletionItem, CompletionCandidate> originals = new IdentityHashMap<>();
        List<CompletionItem> rankable = new ArrayList<>(merged.size());
        for (CompletionCandidate candidate : merged) {
            CompletionItem item = new CompletionItem(candidate.label(), candidate.insertText(), candidate.detail(),
                    completionKind(candidate.kind()), candidate.signature(), candidate.returnType(), candidate.owner(),
                    candidate.documentation(), candidate.example(), candidate.category(), candidate.sortKey());
            rankable.add(item);
            originals.put(item, candidate);
        }
        return new CompletionResult(ranking.rank(rankable, prefix, true).stream()
                .map(originals::get).toList());
    }

    private static com.eyecode.editor.v2.completion.CompletionItemKind completionKind(String kind) {
        try {
            return com.eyecode.editor.v2.completion.CompletionItemKind.valueOf(kind);
        } catch (IllegalArgumentException | NullPointerException ignored) {
            return com.eyecode.editor.v2.completion.CompletionItemKind.VARIABLE;
        }
    }

    private static CompletionCandidate enrichCandidate(CompletionCandidate raw, String prefix,
                                                       int replaceStart, int replaceEnd) {
        String base = baseLabel(raw.label());
        com.eyecode.editor.v2.completion.CompletionItem kbItem =
                com.eyecode.editor.v2.completion.knowledge.JavaKnowledgeBase.get(base);
        if (kbItem == null) {
            kbItem = com.eyecode.editor.v2.completion.database.CompletionDatabase.get(base);
        }
        String documentation = !raw.documentation().isEmpty() ? raw.documentation()
                : kbItem != null && kbItem.getDocumentation() != null ? kbItem.getDocumentation() : "";
        String example = kbItem != null && kbItem.getExample() != null ? kbItem.getExample() : raw.example();
        String category = kbItem != null && kbItem.getCategory() != null ? kbItem.getCategory()
                : !raw.category().isEmpty() ? raw.category()
                : "METHOD".equals(raw.kind()) ? "Method" : "FIELD".equals(raw.kind()) ? "Field" : "";
        String signature = !raw.signature().isEmpty() ? raw.signature()
                : kbItem != null && kbItem.getSignature() != null ? kbItem.getSignature() : base + "()";
        String returnType = !raw.returnType().isEmpty() ? raw.returnType()
                : kbItem != null && kbItem.getReturnType() != null ? kbItem.getReturnType() : "";
        String owner = !raw.owner().isEmpty() ? raw.owner()
                : kbItem != null && kbItem.getOwner() != null ? kbItem.getOwner() : "";

        List<Integer> matchIndices = computeMatchIndices(base, prefix);

        int start = replaceStart > 0 || replaceEnd > 0 ? replaceStart : raw.replaceStart();
        int end = replaceStart > 0 || replaceEnd > 0 ? replaceEnd : raw.replaceEnd();

        return new CompletionCandidate(raw.label(), raw.kind(), raw.detail(), documentation,
                raw.insertText(), raw.filterText(), raw.snippet(), start, end, raw.sortKey(),
                signature, returnType, owner, example, category, matchIndices);
    }

    private static List<Integer> computeMatchIndices(String target, String query) {
        if (target == null || query == null || query.isEmpty()) return List.of();
        String qLow = query.toLowerCase(java.util.Locale.ROOT);
        String tLow = target.toLowerCase(java.util.Locale.ROOT);
        List<Integer> indices = new ArrayList<>();
        int qi = 0;
        for (int ti = 0; ti < tLow.length() && qi < qLow.length(); ti++) {
            if (tLow.charAt(ti) == qLow.charAt(qi)) {
                indices.add(ti);
                qi++;
            }
        }
        return qi == qLow.length() ? List.copyOf(indices) : List.of();
    }

    private static String identity(CompletionCandidate candidate) {
        return candidate.kind() + "\u0000" + baseLabel(candidate.label());
    }

    private static String baseLabel(String label) {
        int open = label.indexOf('(');
        return open > 0 ? label.substring(0, open) : label;
    }

    private static boolean isSubsequence(String query, String candidate) {
        int index = 0;
        String normalizedQuery = query.toLowerCase(java.util.Locale.ROOT);
        String normalizedCandidate = candidate.toLowerCase(java.util.Locale.ROOT);
        for (int cursor = 0; cursor < normalizedCandidate.length() && index < normalizedQuery.length(); cursor++) {
            if (normalizedCandidate.charAt(cursor) == normalizedQuery.charAt(index)) index++;
        }
        return index == normalizedQuery.length();
    }

    private CompletionCandidate candidate(CompletionItem item, int replaceStart, int replaceEnd, String prefix) {
        return new CompletionCandidate(item.getLabel(), item.getKind().name(), item.getDetail(), item.getDocumentation(),
                item.getInsertText(), item.getLabel(), item.getKind() == com.eyecode.editor.v2.completion.CompletionItemKind.SNIPPET,
                replaceStart, replaceEnd, item.getPriority(), item.getSignature(), item.getReturnType(), item.getOwner(),
                item.getExample(), item.getCategory(), engine.matchIndices(item, prefix));
    }
}
