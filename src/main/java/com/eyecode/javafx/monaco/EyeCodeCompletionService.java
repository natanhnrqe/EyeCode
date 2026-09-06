package com.eyecode.javafx.monaco;

import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;
import com.eyecode.editor.intelligence.document.DocumentSnapshot;

import java.util.List;

public final class EyeCodeCompletionService {
    /** Keeps explicit empty-prefix completion useful while bounding frontend pushes. */
    public static final int MAX_RESULTS = 100;
    private final CompletionEngine engine;
    private final boolean markOriginal;

    public EyeCodeCompletionService(CompletionEngine engine) {
        this(engine, false);
    }

    public EyeCodeCompletionService(CompletionEngine engine, boolean markOriginal) {
        this.engine = engine;
        this.markOriginal = markOriginal;
    }

    public List<MonacoCompletionItem> complete(MonacoCompletionRequest request,
                                               LanguageContext context) {
        if (request == null || context == null) return List.of();
        CompletionSnapshot snapshot = engine.complete(context,
                request.explicit()
                        || request.triggerKind() == MonacoCompletionRequest.TriggerKind.TRIGGER_CHARACTER);
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        int caret = context.getDocument().offsetOf(context.getCaret());
        int replaceStart = replacementStart(request, context.getDocument().snapshot(), caret, prefix);
        int replaceEnd = replacementEnd(request, context.getDocument().snapshot(), caret);
        return snapshot.getItems().stream()
                .limit(MAX_RESULTS)
                .map(item -> mappedItem(item, replaceStart, replaceEnd, prefix))
                .toList();
    }

    private MonacoCompletionItem mappedItem(CompletionItem item, int replaceStart, int replaceEnd,
                                            String prefix) {
        MonacoCompletionItem mapped = MonacoCompletionItem.from(item, replaceStart, replaceEnd,
                engine.matchIndices(item, prefix));
        if (!markOriginal) return mapped;
        return new MonacoCompletionItem(mapped.label(), mapped.kind(), "EYECODE_ORIGINAL",
                mapped.documentation(), mapped.insertText(), mapped.filterText(), mapped.snippet(),
                mapped.replaceStart(), mapped.replaceEnd(), mapped.sortKey(), mapped.signature(),
                mapped.returnType(), mapped.owner(), mapped.example(), mapped.category(), mapped.matchIndices());
    }

    private static int replacementStart(MonacoCompletionRequest request, DocumentSnapshot snapshot,
                                        int caret, String prefix) {
        if (request.replaceStart() >= 0 && request.replaceStart() <= caret) {
            return clamp(snapshot, request.replaceStart());
        }
        return Math.max(0, caret - prefix.length());
    }

    private static int replacementEnd(MonacoCompletionRequest request, DocumentSnapshot snapshot,
                                      int caret) {
        if (request.replaceEnd() >= caret) {
            return clamp(snapshot, request.replaceEnd());
        }
        return caret;
    }

    private static int clamp(DocumentSnapshot snapshot, int offset) {
        return Math.max(0, Math.min(offset, snapshot.text().length()));
    }
}
