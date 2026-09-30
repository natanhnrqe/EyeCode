package com.eyecode.language.java.lsp;

import java.util.List;

public record JdtLsCompletionResolveResult(List<JdtLsTextEdit> additionalTextEdits, String documentation) {
    public JdtLsCompletionResolveResult {
        additionalTextEdits = additionalTextEdits == null ? List.of() : List.copyOf(additionalTextEdits);
        documentation = documentation == null ? "" : documentation;
    }

    public static JdtLsCompletionResolveResult empty() {
        return new JdtLsCompletionResolveResult(List.of(), "");
    }
}
