package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class CompletionEngine {

    private final List<CompletionProvider> providers;
    private final CompletionRanking ranking;

    public CompletionEngine(List<CompletionProvider> providers) {
        this.providers = List.copyOf(providers);
        this.ranking = new CompletionRanking();
    }

    public CompletionSnapshot complete(LanguageContext context) {
        return complete(context, false);
    }

    public CompletionSnapshot complete(LanguageContext context, boolean manual) {
        if (context == null || isSuppressedContext(context)) {
            return CompletionSnapshot.empty();
        }
        CompletionContextKind contextKind = CompletionContextResolver.resolve(context);
        Map<String, CompletionItem> merged = new LinkedHashMap<>();
        for (CompletionProvider provider : providers) {
            if (!provider.supports(contextKind)) {
                continue;
            }
            CompletionSnapshot snapshot = provider.complete(context, manual);
            for (CompletionItem item : snapshot.getItems()) {
                merged.putIfAbsent(item.getLabel() + "\u0000" + item.getKind(), item);
            }
        }
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        List<CompletionItem> candidates = new ArrayList<>(merged.values());
        if (CompletionContextResolver.isMethodBodyExpressionContext(context)) {
            candidates.removeIf(item -> item.getKind() == CompletionItemKind.KEYWORD
                    && Set.of("public", "private", "protected").contains(item.getLabel()));
        }
        List<CompletionItem> ranked = ranking.rank(candidates, prefix,
                manual || contextKind == CompletionContextKind.MEMBER_ACCESS);
        return new CompletionSnapshot(ranked);
    }

    public List<Integer> matchIndices(CompletionItem item, String query) {
        return ranking.matchIndices(item, query);
    }

    private boolean isSuppressedContext(LanguageContext context) {
        int offset = context.getDocument().offsetOf(context.getCaret());
        for (SyntaxToken token : context.getSyntax().getTokens()) {
            if (token.startOffset() <= offset && offset <= token.endOffset()) {
                return token.type() == TokenType.COMMENT || token.type() == TokenType.STRING;
            }
        }
        return false;
    }
}
