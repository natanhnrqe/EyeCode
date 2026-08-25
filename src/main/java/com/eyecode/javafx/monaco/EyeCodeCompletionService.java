package com.eyecode.javafx.monaco;

import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;

import java.util.List;

public final class EyeCodeCompletionService {
    private final CompletionEngine engine;

    public EyeCodeCompletionService(CompletionEngine engine) {
        this.engine = engine;
    }

    public List<MonacoCompletionItem> complete(MonacoCompletionRequest request,
                                               LanguageContext context) {
        if (request == null || context == null) return List.of();
        CompletionSnapshot snapshot = engine.complete(context,
                request.triggerKind() == MonacoCompletionRequest.TriggerKind.INVOKED);
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        int caret = context.getDocument().offsetOf(context.getCaret());
        int replaceStart = Math.max(0, caret - prefix.length());
        return snapshot.getItems().stream()
                .map(item -> MonacoCompletionItem.from(item, replaceStart, caret))
                .toList();
    }
}
