package com.eyecode.editor.v2.completion;

import com.eyecode.editor.v2.completion.database.CompletionDatabase;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.completion.insert.CompletionPrefixResolver;

import java.util.List;

public final class JavaStandardLibraryProvider implements CompletionProvider {

    private static final List<CompletionItem> LIBRARY_ITEMS = CompletionDatabase.getAll();

    @Override
    public CompletionSnapshot complete(LanguageContext context) {
        return complete(context, false);
    }

    @Override
    public CompletionSnapshot complete(LanguageContext context, boolean manual) {
        if (CompletionPrefixResolver.isQualifiedContext(context)) {
            return qualifiedItems(context);
        }
        String prefix = CompletionPrefixResolver.resolvePrefix(context);
        if (prefix.isEmpty() && !manual) {
            return CompletionSnapshot.empty();
        }

        List<CompletionItem> items = LIBRARY_ITEMS.stream()
                .filter(item -> item.getLabel().startsWith(prefix))
                .toList();

        return new CompletionSnapshot(items);
    }

    private CompletionSnapshot qualifiedItems(LanguageContext context) {
        String text = context.getDocument().getText();
        int offset = context.getDocument().offsetOf(context.getCaret());
        int safeOffset = Math.max(0, Math.min(offset, text.length()));
        int prefixStart = safeOffset;
        while (prefixStart > 0 && Character.isJavaIdentifierPart(text.charAt(prefixStart - 1))) {
            prefixStart--;
        }
        int dot = prefixStart - 1;
        if (dot < 0 || text.charAt(dot) != '.') {
            return CompletionSnapshot.empty();
        }
        int receiverStart = dot;
        while (receiverStart > 0 && Character.isJavaIdentifierPart(text.charAt(receiverStart - 1))) {
            receiverStart--;
        }
        String receiver = text.substring(receiverStart, dot);
        String prefix = text.substring(prefixStart, safeOffset);
        if (receiver.isBlank()) {
            return CompletionSnapshot.empty();
        }

        List<CompletionItem> items = LIBRARY_ITEMS.stream()
                .filter(item -> item.getDetail() != null
                        && item.getDetail().startsWith(receiver + "."))
                .filter(item -> qualifiedLabel(item, receiver)
                        .toLowerCase(java.util.Locale.ROOT)
                        .startsWith(prefix.toLowerCase(java.util.Locale.ROOT)))
                .toList();
        return new CompletionSnapshot(items);
    }

    private String qualifiedLabel(CompletionItem item, String receiver) {
        String detail = item.getDetail();
        String qualifiedPrefix = receiver + ".";
        if (detail != null && detail.startsWith(qualifiedPrefix)) {
            String suffix = detail.substring(qualifiedPrefix.length());
            int separator = suffix.indexOf('.');
            return separator >= 0 ? suffix.substring(0, separator) : suffix;
        }
        return item.getLabel();
    }
}
