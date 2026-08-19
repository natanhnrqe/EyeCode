package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, CompletionItem> merged = new LinkedHashMap<>();
        for (CompletionProvider provider : providers) {
            CompletionSnapshot snapshot = provider.complete(context, manual);
            for (CompletionItem item : snapshot.getItems()) {
                merged.putIfAbsent(item.getLabel() + "\u0000" + item.getKind(), item);
            }
        }
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        List<CompletionItem> ranked = ranking.rank(new ArrayList<>(merged.values()), prefix, manual);
        return new CompletionSnapshot(ranked);
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
