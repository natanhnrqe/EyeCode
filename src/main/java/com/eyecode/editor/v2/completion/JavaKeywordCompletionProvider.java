package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.language.LanguageContextQueries;

import java.util.List;

public final class JavaKeywordCompletionProvider implements CompletionProvider {

    private static final List<String> KEYWORDS = List.of(
            "public",
            "private",
            "protected",
            "class",
            "interface",
            "enum",
            "extends",
            "implements",
            "static",
            "final",
            "void",
            "new",
            "return",
            "if",
            "else",
            "for",
            "while",
            "switch",
            "case",
            "try",
            "catch"
    );

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        String prefix = LanguageContextQueries.getCurrentWordPrefix(context);
        if (prefix.isEmpty()) {
            return CompletionSnapshot.empty();
        }

        List<CompletionItem> items = KEYWORDS.stream()
                .filter(keyword -> keyword.startsWith(prefix))
                .map(keyword -> new CompletionItem(
                        keyword,
                        keyword,
                        "Java keyword",
                        CompletionItemKind.KEYWORD
                ))
                .toList();

        return new CompletionSnapshot(items);
    }
}
